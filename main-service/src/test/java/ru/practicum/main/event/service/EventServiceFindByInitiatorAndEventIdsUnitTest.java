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
import ru.practicum.main.event.dto.EventFullDto;
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
class EventServiceFindByInitiatorAndEventIdsUnitTest {
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
    private Event event;

    private static final Long USER_ID = 100L;
    private static final Long EVENT_ID = 1L;

    @BeforeEach
    void setUp() {
        EasyRandom easyRandom = TestDataUtils.createEventEasyRandomizer();

        user = easyRandom.nextObject(User.class);
        user.setId(USER_ID);

        event = easyRandom.nextObject(Event.class);
        event.setId(EVENT_ID);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Успешный возврат события со всеми данными")
    void shouldReturnEventFullDtoWithAllData() {
        List<Long> eventIds = List.of(EVENT_ID);
        List<String> uris = eventIds.stream().map(id -> String.format("/events/%d", id)).toList();
        Long views = 999L;
        Long confirmedRequests = 1L;

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID))
                .thenReturn(Optional.of(event));

        when(statRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(true)))
                .thenReturn(getViewStatsDto(eventIds, views));

        /*when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(List.of(new ConfirmedRequestsCount(EVENT_ID, confirmedRequests)));
        верни потом на место Сахар*/
        EventFullDto result = eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID);

        assertThat(result).isNotNull()
                .returns(EVENT_ID, EventFullDto::getId)
                .returns(event.getTitle(), EventFullDto::getTitle)
                .returns(event.getAnnotation(), EventFullDto::getAnnotation)
                .returns(event.getPaid(), EventFullDto::getPaid)
                .returns(event.getEventDate(), EventFullDto::getEventDate)
                .returns(event.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(event.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(views, EventFullDto::getViews)
                .returns(confirmedRequests, EventFullDto::getConfirmedRequests);

        verify(userRepository, times(1))
                .findById(USER_ID);
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(EVENT_ID, USER_ID);
        verify(statRepository, times(1))
                .getStats(argThat(start -> start != null && !start.isAfter(event.getCreatedOn())), any(LocalDateTime.class), eq(uris), eq(true));
        /*verify(requestRepository, times(1))
                .countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);
        верни на место Сахар*/
        verifyNoMoreInteractions(userRepository, eventRepository, statRepository, requestRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Успешный возврат события с нулевой статистикой")
    void shouldReturnEventWithZeroStatistics() {
        List<Long> eventIds = List.of(EVENT_ID);
        List<String> uris = eventIds.stream().map(id -> String.format("/events/%d", id)).toList();
        Long views = 0L;
        Long confirmedRequests = 0L;

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID))
                .thenReturn(Optional.of(event));

        when(statRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(true)))
                .thenReturn(Collections.emptyList());

        /*when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());
        верни на место сахар*/
        EventFullDto result = eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID);

        assertThat(result).isNotNull()
                .returns(EVENT_ID, EventFullDto::getId)
                .returns(event.getTitle(), EventFullDto::getTitle)
                .returns(event.getAnnotation(), EventFullDto::getAnnotation)
                .returns(event.getPaid(), EventFullDto::getPaid)
                .returns(event.getEventDate(), EventFullDto::getEventDate)
                .returns(event.getCategory().getId(), dto -> dto.getCategory().getId())
                .returns(event.getInitiator().getId(), dto -> dto.getInitiator().getId())
                .returns(views, EventFullDto::getViews)
                .returns(confirmedRequests, EventFullDto::getConfirmedRequests);

        verify(userRepository, times(1))
                .findById(USER_ID);
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(EVENT_ID, USER_ID);
        verify(statRepository, times(1))
                .getStats(argThat(start -> start != null && !start.isAfter(event.getCreatedOn())), any(LocalDateTime.class), eq(uris), eq(true));
        /*verify(requestRepository, times(1))
                .countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);
        */
        verifyNoMoreInteractions(userRepository, eventRepository, statRepository, requestRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("NotFoundException если пользователь не найден")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("fieldName", "UserId")
                .hasFieldOrPropertyWithValue("rejectedValue", USER_ID);

        verify(userRepository, times(1)).findById(USER_ID);

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(eventRepository, categoryRepository, statRepository, requestRepository);
    }

    @Test
    @DisplayName("NotFoundException если у пользователя не найден ивент")
    void shouldThrowNotFoundExceptionWhenUserHasNoSuchEvent() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("fieldName", "EventId")
                .hasFieldOrPropertyWithValue("rejectedValue", EVENT_ID);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(EVENT_ID, USER_ID);


        verifyNoMoreInteractions(userRepository, eventRepository);
        verifyNoInteractions(categoryRepository, statRepository, requestRepository);
    }

    private List<ViewStatsDto> getViewStatsDto(List<Long> eventIds, Long views) {
        return eventIds.stream().map(id -> String.format("/events/%d", id)).map(uri -> new ViewStatsDto("ewm", uri, views)).toList();
    }
}
