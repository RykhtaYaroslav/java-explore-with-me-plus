package ru.practicum.main.event.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ConfirmedRequestsCount;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.main.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceFindByInitiatorIdUnitTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatsClient statRepository;

    // MapStruct warns against using Mappers.getMapper() for Spring-managed mappers.
    // Suppressed here because factory instantiation is required to spy on the real mapper in unit tests without loading the Spring context.
    @Spy
    @SuppressWarnings("all")
    private EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Event firstEvent;
    private Event secondEvent;

    private static final Long USER_ID = 100L;
    private static final Long FIRST_EVENT_ID = 1L;
    private static final Long SECOND_EVENT_ID = 2L;

    @BeforeEach
    void setUp() {
        EasyRandom easyRandom = TestDataUtils.createEventEasyRandomizer();

        user = easyRandom.nextObject(User.class);
        user.setId(USER_ID);

        firstEvent = easyRandom.nextObject(Event.class);
        firstEvent.setId(FIRST_EVENT_ID);
        firstEvent.setInitiator(user);
        firstEvent.setCreatedOn(LocalDateTime.now().minusDays(2));

        secondEvent = easyRandom.nextObject(Event.class);
        secondEvent.setId(SECOND_EVENT_ID);
        secondEvent.setInitiator(user);
        secondEvent.setCreatedOn(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Успешно возвращает лист ивентов по id пользователя")
    void shouldReturnListOfShortDtoByInitiatorId() {
        List<Event> events = List.of(firstEvent, secondEvent);
        List<Long> eventIds = List.of(FIRST_EVENT_ID, SECOND_EVENT_ID);
        List<String> uris = eventIds.stream().map( id -> String.format("/events/%d", id)).toList();

        Long views = 999L;

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByInitiatorId(eq(USER_ID), any(Integer.class), any(Integer.class)))
                .thenReturn(events);
        when(statRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(true)))
                .thenReturn(getViewStatsDto(eventIds, views));
        when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(List.of(new ConfirmedRequestsCount(FIRST_EVENT_ID, 1L), new ConfirmedRequestsCount(SECOND_EVENT_ID, 2L)));

        List<EventShortDto> result = eventService.findAllByInitiatorId(USER_ID, 0, 10);

        assertThat(result)
                .hasSize(2)
                .extracting(EventShortDto::getId)
                .containsExactly(FIRST_EVENT_ID, SECOND_EVENT_ID);

        assertThat(result.getFirst())
                .returns(FIRST_EVENT_ID, EventShortDto::getId)
                .returns(firstEvent.getTitle(), EventShortDto::getTitle)
                .returns(firstEvent.getAnnotation(), EventShortDto::getAnnotation)
                .returns(firstEvent.getPaid(), EventShortDto::getPaid)
                .returns(firstEvent.getEventDate(), EventShortDto::getEventDate)
                .returns(firstEvent.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(firstEvent.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(views, EventShortDto::getViews)
                .returns(1L, EventShortDto::getConfirmedRequests);

        assertThat(result.getLast())
                .returns(SECOND_EVENT_ID, EventShortDto::getId)
                .returns(secondEvent.getTitle(), EventShortDto::getTitle)
                .returns(secondEvent.getAnnotation(), EventShortDto::getAnnotation)
                .returns(secondEvent.getPaid(), EventShortDto::getPaid)
                .returns(secondEvent.getEventDate(), EventShortDto::getEventDate)
                .returns(secondEvent.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(secondEvent.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(views, EventShortDto::getViews)
                .returns(2L, EventShortDto::getConfirmedRequests);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByInitiatorId(eq(USER_ID), Mockito.anyInt(), Mockito.anyInt());
        verify(statRepository, times(1)).getStats(argThat(start -> start != null && !start.isAfter(firstEvent.getCreatedOn())), any(LocalDateTime.class), eq(uris), eq(true));
        verify(requestRepository, times(1)).countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);

        verifyNoMoreInteractions(userRepository, eventRepository, statRepository, requestRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Возврат DTO с нулевыми просмотрами и заявками, если их не было")
    void shouldReturnZeroViewsAndRequestsWhenStatsEmpty() {
        List<Event> events = List.of(firstEvent, secondEvent);
        List<Long> eventIds = List.of(FIRST_EVENT_ID, SECOND_EVENT_ID);
        List<String> uris = eventIds.stream().map( id -> String.format("/events/%d", id)).toList();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByInitiatorId(eq(USER_ID), any(Integer.class), any(Integer.class)))
                .thenReturn(events);
        when(statRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(true)))
                .thenReturn(Collections.emptyList());
        when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        List<EventShortDto> result = eventService.findAllByInitiatorId(USER_ID, 0, 10);

        assertThat(result)
                .hasSize(2)
                .extracting(EventShortDto::getId)
                .containsExactly(FIRST_EVENT_ID, SECOND_EVENT_ID);

        assertThat(result.getFirst())
                .returns(FIRST_EVENT_ID, EventShortDto::getId)
                .returns(firstEvent.getTitle(), EventShortDto::getTitle)
                .returns(firstEvent.getAnnotation(), EventShortDto::getAnnotation)
                .returns(firstEvent.getPaid(), EventShortDto::getPaid)
                .returns(firstEvent.getEventDate(), EventShortDto::getEventDate)
                .returns(firstEvent.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(firstEvent.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(0L, EventShortDto::getViews)
                .returns(0L, EventShortDto::getConfirmedRequests);

        assertThat(result.getLast())
                .returns(SECOND_EVENT_ID, EventShortDto::getId)
                .returns(secondEvent.getTitle(), EventShortDto::getTitle)
                .returns(secondEvent.getAnnotation(), EventShortDto::getAnnotation)
                .returns(secondEvent.getPaid(), EventShortDto::getPaid)
                .returns(secondEvent.getEventDate(), EventShortDto::getEventDate)
                .returns(secondEvent.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(secondEvent.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(0L, EventShortDto::getViews)
                .returns(0L, EventShortDto::getConfirmedRequests);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByInitiatorId(eq(USER_ID), Mockito.anyInt(), Mockito.anyInt());
        verify(statRepository, times(1)).getStats(argThat(start -> start != null && !start.isAfter(firstEvent.getCreatedOn())), any(LocalDateTime.class), eq(uris), eq(true));
        verify(requestRepository, times(1)).countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);

        verifyNoMoreInteractions(userRepository, eventRepository, statRepository, requestRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Возврат пустого списка, если пользователь не добавлял никаких событий")
    void shouldReturnEmptyListWhenUserHasNoEvents() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByInitiatorId(eq(USER_ID), any(Integer.class), any(Integer.class)))
                .thenReturn(Collections.emptyList());

        List<EventShortDto> result = eventService.findAllByInitiatorId(USER_ID, 0, 10);

        assertThat(result).isEmpty();

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByInitiatorId(eq(USER_ID), Mockito.anyInt(), Mockito.anyInt());

        verifyNoMoreInteractions(userRepository, eventRepository);
        verifyNoInteractions(categoryRepository, statRepository, requestRepository);
    }

    @Test
    @DisplayName("NotFoundException если пользователь не найден")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findAllByInitiatorId(USER_ID, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("fieldName", "UserId")
                .hasFieldOrPropertyWithValue("rejectedValue", USER_ID);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);

        Mockito.verifyNoMoreInteractions(userRepository);
        Mockito.verifyNoInteractions(eventRepository, categoryRepository, statRepository, requestRepository);
    }

    private List<ViewStatsDto> getViewStatsDto(List<Long> eventIds, Long views) {
        return eventIds.stream().map(id -> String.format("/events/%d", id)).map(uri -> new ViewStatsDto("ewm", uri, views)).toList();
    }
}
