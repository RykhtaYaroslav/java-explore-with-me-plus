package ru.practicum.main.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.StateActionUser;
import ru.practicum.main.event.dto.UpdateEventUserRequest;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.EventUpdateException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ConfirmedRequestsCount;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static ru.practicum.main.util.EwmConstants.DATE_TIME_FORMATTER;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsRepository;

    private final EventMapper eventMapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User initiator = getUser(userId);

        Long categoryId = newEventDto.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException((String.format("Category with id = %d not found", categoryId)), "CategoryId", categoryId));

        Event event = eventMapper.toEntity(newEventDto, initiator, category);

        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, 0L, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size) {
        getUser(userId); // only for user existence checking

        List<Event> events = eventRepository.findByInitiatorId(userId, from, size);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsByEventMap = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequestsByEventMap = getConReqByEventMap(events);

        return events.stream()
                .map(event -> saturateEventShortDto(event, viewsByEventMap, confirmedRequestsByEventMap)).toList();
    }

    /**
     * Retrieves full details of a specific event created by a specific initiator.
     *
     * @param userId  the unique identifier of the event initiator
     * @param eventId the unique identifier of the requested event
     * @return the populated {@link EventFullDto} with views and confirmed requests
     * @throws NotFoundException if user or event does not exist, or event does not belong to the user
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId) {
        getUser(userId); // only for user existence checking

        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access

        List<Event> events = List.of(event);
        Map<Long, Long> views = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequests = getConReqByEventMap(events);

        return eventMapper.toFullDto(event, views.getOrDefault(eventId, 0L), confirmedRequests.getOrDefault(eventId, 0L));
    }

    /**
     * Updates an existing event by its initiator.
     * <p>
     * Validates the event's eligibility for update, applies non-null fields from the request DTO,
     * saves the updated entity, and enriches the result with current views and confirmed requests.
     *
     * @param request the DTO containing updated event details
     * @param userId  the ID of the user initiating the update
     * @param eventId the ID of the event to be updated
     * @return the updated {@link EventFullDto} containing full event details and updated statistics
     * @throws NotFoundException    if the user, event, or specified category is not found
     * @throws EventUpdateException if the event is already published or scheduled to start within 2 hours
     */
    @Override
    public EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId) {
        // Retrieves and validates event: throws NotFoundException if user/event doesn't exist,
        // and EventUpdateException if event is PUBLISHED or starts in less than 2 hours
        Event event = getEventIfValidToUpdate(request, userId, eventId);

        // Updates event entity with non-null fields from request DTO, including category and state transition
        updateEventFromNotNullDtoFields(request, event);

        eventRepository.save(event);

        List<Event> events = List.of(event);
        Map<Long, Long> views = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequests = getConReqByEventMap(events);

        return eventMapper.toFullDto(event, views.getOrDefault(eventId, 0L), confirmedRequests.getOrDefault(eventId, 0L));
    }

    /**
     * Retrieves a {@link User} entity by its ID from the repository.
     *
     * @param userId the unique identifier of the user to fetch
     * @return the found {@link User} entity
     * @throws NotFoundException if no user with the specified ID exists
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", userId), "UserId", userId));
    }

    /**
     * Maps each event ID to its total number of views collected from the statistics service.
     *
     * @param events the list of {@link Event} entities to fetch statistics for
     * @return a {@link Map} where the key is the event ID and the value is the total view count
     */
    private Map<Long, Long> getViewsByEventMap(List<Event> events) {
        // This method fills Map with eventId - views amount

        // Pull statistics about events from stat module
        List<ViewStatsDto> viewStatsDto = getViewStatsDtoList(events);

        return viewStatsDto.stream()
                .collect(Collectors.toMap(
                        dto -> extractIdFromUri(dto.getUri()),
                        ViewStatsDto::getHits,
                        Long::max
                ));

    }

    /**
     * Requests view statistics from the remote stats service for a collection of events.
     *
     * @param events the list of {@link Event} entities for which to query view statistics
     * @return a list of {@link ViewStatsDto} containing hit counts for the provided event URIs
     */
    private List<ViewStatsDto> getViewStatsDtoList(List<Event> events) {
        // This method pulls statistics about events from stat module

        LocalDateTime now = now();

        List<String> uris = events.stream()
                .map(event -> String.format("/events/%d", event.getId()))
                .toList();

        LocalDateTime minDate = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(now.minusYears(10));

        return statsRepository.getStats(minDate, now, uris, true);
    }

    /**
     * Extracts the event entity ID from an endpoint URI string (e.g., "/events/42" -> 42L).
     *
     * @param uri the URI path containing the event identifier at the end
     * @return the extracted event ID as a {@link Long}
     */
    private Long extractIdFromUri(String uri) {
        return Long.parseLong(uri.substring((uri.lastIndexOf('/') + 1)));
    }

    /**
     * Maps each event ID to the total number of its confirmed participation requests.
     *
     * @param events the list of {@link Event} entities to aggregate requests for
     * @return a {@link Map} where the key is the event ID and the value is the confirmed request count
     */
    private Map<Long, Long> getConReqByEventMap(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        List<ConfirmedRequestsCount> confirmedRequestsCounts = requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);

        return confirmedRequestsCounts.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCount::eventId,
                        ConfirmedRequestsCount::count
                ));
    }

    /**
     * Enriches an {@link Event} entity with its calculated views and confirmed requests
     * and maps it to a short DTO representation.
     *
     * @param event                       the event entity to map
     * @param viewsByEventMap             a map containing event IDs and their view counts
     * @param confirmedRequestsByEventMap a map containing event IDs and their confirmed request counts
     * @return the fully populated {@link EventShortDto}
     */
    private EventShortDto saturateEventShortDto(Event event, Map<Long, Long> viewsByEventMap, Map<Long, Long> confirmedRequestsByEventMap) {
        Long views = viewsByEventMap.getOrDefault(event.getId(), 0L);
        Long confirmedRequests = confirmedRequestsByEventMap.getOrDefault(event.getId(), 0L);

        return eventMapper.toShortDto(event, views, confirmedRequests);
    }

    /**
     * Retrieves an {@link Event} entity by its ID and ensures it belongs to the specified initiator.
     *
     * @param userId  the unique identifier of the event initiator
     * @param eventId the unique identifier of the event to fetch
     * @return the found {@link Event} entity
     * @throws NotFoundException if the event does not exist or does not belong to the specified user
     */
    private Event getEventByIdAndInitiator(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d not found for user with id = %d", eventId, userId),
                        "EventId", eventId));
    }

    /**
     * Retrieves an event by its ID and initiator ID after validating that it exists,
     * belongs to the user, and meets all prerequisites for updating.
     * <p>
     * Validates that the event is not in a {@code PUBLISHED} state, and that the event date
     * is at least 2 hours in the future (unless a valid new date is provided in the request).
     *
     * @param request the DTO containing updated event details
     * @param userId  the ID of the user requesting the update
     * @param eventId the ID of the event to be updated
     * @return the validated {@link Event} entity ready for modification
     * @throws NotFoundException    if either the user or the event is not found
     * @throws EventUpdateException if the event is published or scheduled within 2 hours without a valid date postponement
     */
    private Event getEventIfValidToUpdate(UpdateEventUserRequest request, Long userId, Long eventId) {
        getUser(userId); // only for user existence checking
        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access

        if (event.getState() == EventState.PUBLISHED) {
            String message = String.format("Event with id = %d cannot be updated because its status is PUBLISHED", eventId);
            throw new EventUpdateException(message, "State", event.getState());
        }

        LocalDateTime deadline = now().plusHours(2);
        if (event.getEventDate().isBefore(deadline) && ((request.getEventDate() == null) || request.getEventDate().isBefore(deadline))) {
            String message = String.format("Event with id = %d cannot be updated because event date %s is less than 2 hours from now", eventId, event.getEventDate().format(DATE_TIME_FORMATTER));
            throw new EventUpdateException(
                    message,
                    "EventDate",
                    event.getEventDate()
            );
        }
        return event;
    }

    /**
     * Updates non-null fields of the existing {@link Event} entity based on the provided {@link UpdateEventUserRequest}.
     * <p>
     * Performs partial mapping of basic scalar fields via {@link EventMapper}, fetches and assigns
     * a new {@link Category} if {@code categoryId} is present, and updates the {@link EventState}
     * according to the requested {@link StateActionUser}.
     *
     * @param request the DTO containing updated event details
     * @param event   the target {@link Event} entity to be updated
     * @throws NotFoundException if the category specified by {@code categoryId} does not exist
     */
    private void updateEventFromNotNullDtoFields(UpdateEventUserRequest request, Event event) {
        eventMapper.updateEventFromUserDto(request, event);

        if (request.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new NotFoundException(
                    String.format("Category with id = %d was not found", request.getCategoryId()),
                    "CategoryId",
                    request.getCategoryId()
            ));
            event.setCategory(newCategory);
        }
        StateActionUser stateActionUser = request.getStateAction();

        switch (stateActionUser) {
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case null, default -> {/*Do nothing*/}
        }
    }
}
