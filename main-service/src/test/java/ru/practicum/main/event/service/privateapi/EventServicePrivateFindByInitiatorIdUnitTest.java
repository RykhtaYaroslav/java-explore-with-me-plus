package ru.practicum.main.event.service.privateapi;

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
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.main.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateFindByInitiatorIdUnitTest {
    private static final Long USER_ID = 100L;
    private static final Long FIRST_EVENT_ID = 1L;
    private static final Long SECOND_EVENT_ID = 2L;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private StatsClient statsRepository;
    @Mock
    private EventStatsCollector eventStatsCollector;

    @Spy
    @SuppressWarnings("all")
    private EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @InjectMocks
    private EventServicePrivateImpl eventService;

    private User user;
    private Event firstEvent;
    private Event secondEvent;

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

        EventShortDto shortDto1 = EventShortDto.builder()
                .id(FIRST_EVENT_ID)
                .title(firstEvent.getTitle())
                .annotation(firstEvent.getAnnotation())
                .paid(firstEvent.getPaid())
                .eventDate(firstEvent.getEventDate())
                .category(CategoryDto.builder().id(firstEvent.getCategory().getId()).build())
                .initiator(UserShortDto.builder().id(USER_ID).build())
                .views(999L)
                .confirmedRequests(1L)
                .build();

        EventShortDto shortDto2 = EventShortDto.builder()
                .id(SECOND_EVENT_ID)
                .title(secondEvent.getTitle())
                .annotation(secondEvent.getAnnotation())
                .paid(secondEvent.getPaid())
                .eventDate(secondEvent.getEventDate())
                .category(CategoryDto.builder().id(secondEvent.getCategory().getId()).build())
                .initiator(UserShortDto.builder().id(USER_ID).build())
                .views(999L)
                .confirmedRequests(2L)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByInitiatorId(eq(USER_ID), any(Integer.class), any(Integer.class))).thenReturn(events);
        when(eventStatsCollector.getShortDtoListWithStats(events)).thenReturn(List.of(shortDto1, shortDto2));

        List<EventShortDto> result = eventService.findAllByInitiatorId(USER_ID, 0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isEqualTo(shortDto1);
        assertThat(result.getLast()).isEqualTo(shortDto2);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByInitiatorId(eq(USER_ID), Mockito.anyInt(), Mockito.anyInt());
        verify(eventStatsCollector, times(1)).getShortDtoListWithStats(events);

        verifyNoMoreInteractions(userRepository, eventRepository, eventStatsCollector);
        verifyNoInteractions(categoryRepository, requestRepository, statsRepository);
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
        verifyNoInteractions(categoryRepository, statsRepository, requestRepository, eventStatsCollector);
    }

    @Test
    @DisplayName("NotFoundException если пользователь не найден")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findAllByInitiatorId(USER_ID, 0, 10))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, times(1)).findById(USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(eventRepository, categoryRepository, statsRepository, requestRepository, eventStatsCollector);
    }
}