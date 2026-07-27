package ru.practicum.main.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
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
     * @param userId the unique identifier of the event initiator
     * @param eventId the unique identifier of the requested event
     * @return the populated {@link EventFullDto} with views and confirmed requests
     * @throws NotFoundException if user or event does not exist, or event does not belong to the user
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId) {
        getUser(userId); // only for user existence checking

        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if incorrect

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

        LocalDateTime now = LocalDateTime.now();

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
     * @param userId the unique identifier of the event initiator
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
}
