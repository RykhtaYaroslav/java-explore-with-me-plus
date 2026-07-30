package ru.practicum.main.event.service.adminapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventQueryParams;
import ru.practicum.main.event.dto.admin.StateActionAdmin;
import ru.practicum.main.event.dto.admin.UpdateEventAdminRequest;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.EventUpdateException;
import ru.practicum.main.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceAdminUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventStatsCollector eventStatsCollector;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private EventServiceAdminImpl eventServiceAdmin;

    private Event pendingEvent;
    private Event publishedEvent;
    private Event canceledEvent;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(10L, "Main Category");

        pendingEvent = Event.builder()
                .id(1L)
                .title("Pending Event")
                .state(EventState.PENDING)
                .category(category)
                .eventDate(LocalDateTime.now().plusDays(2))
                .build();

        publishedEvent = Event.builder()
                .id(2L)
                .title("Published Event")
                .state(EventState.PUBLISHED)
                .category(category)
                .publishedOn(LocalDateTime.now().minusDays(1))
                .eventDate(LocalDateTime.now().plusDays(2))
                .build();

        canceledEvent = Event.builder()
                .id(3L)
                .title("Canceled Event")
                .state(EventState.CANCELED)
                .category(category)
                .eventDate(LocalDateTime.now().plusDays(2))
                .build();
    }

    // Тесты метода getAllFullInfoWithParams

    @Test
    @DisplayName("getAllFullInfoWithParams: успешное получение списка событий со всеми фильтрами")
    void getAllFullInfoWithParams_shouldReturnList() {
        EventQueryParams params = EventQueryParams.ofAdmin(
                List.of(1L),
                List.of(EventState.PENDING),
                List.of(10L),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5),
                0,
                10
        );

        EventFullDto fullDto = EventFullDto.builder().id(1L).title("Pending Event").build();

        when(eventRepository.findAllWithParams(
                eq(null),
                eq(List.of(10L)),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(false),
                eq(null),
                eq(List.of(1L)),
                eq(List.of("PENDING")),
                eq(0),
                eq(10)
        )).thenReturn(List.of(pendingEvent));

        when(eventStatsCollector.getFullDtoListWithStats(List.of(pendingEvent))).thenReturn(List.of(fullDto));

        List<EventFullDto> result = eventServiceAdmin.getAllFullInfoWithParams(params);

        assertThat(result)
                .hasSize(1)
                .containsExactly(fullDto);

        verify(eventStatsCollector).getFullDtoListWithStats(List.of(pendingEvent));
    }

    @Test
    @DisplayName("getAllFullInfoWithParams: когда states = null, корректно передает null в репозиторий")
    void getAllFullInfoWithParams_whenStatesNull_shouldPassNullToRepository() {
        EventQueryParams params = EventQueryParams.ofAdmin(
                null, null, null, null, null, 0, 10
        );

        when(eventRepository.findAllWithParams(
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(false), eq(null), eq(null), eq(null), eq(0), eq(10)
        )).thenReturn(List.of());

        when(eventStatsCollector.getFullDtoListWithStats(List.of())).thenReturn(List.of());

        List<EventFullDto> result = eventServiceAdmin.getAllFullInfoWithParams(params);

        assertThat(result).isEmpty();
        verify(eventRepository).findAllWithParams(
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(false), eq(null), eq(null), eq(null), eq(0), eq(10)
        );
    }

    // =========================================================================
    // Тесты метода updateEvent
    // =========================================================================

    @Test
    @DisplayName("updateEvent: успешное редактирование данных и публикация события (PUBLISH_EVENT)")
    void updateEvent_whenPublishEvent_shouldSetPublishedState() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .title("Updated Title")
                .stateAction(StateActionAdmin.PUBLISH_EVENT)
                .build();

        EventFullDto expectedDto = EventFullDto.builder()
                .id(eventId)
                .title("Updated Title")
                .state(EventState.PUBLISHED)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(eventRepository.save(pendingEvent)).thenReturn(pendingEvent);
        when(eventStatsCollector.getFullDtoListWithStats(List.of(pendingEvent))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventServiceAdmin.updateEvent(eventId, request);

        assertThat(result).isNotNull().isEqualTo(expectedDto);
        assertThat(pendingEvent.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(pendingEvent.getPublishedOn()).isNotNull();

        verify(eventMapper).updateEventFromAdminDto(request, pendingEvent);
        verify(eventRepository).save(pendingEvent);
    }

    @Test
    @DisplayName("updateEvent: успешная отмена события (REJECT_EVENT)")
    void updateEvent_whenRejectEvent_shouldSetCanceledState() {
        Long eventId = 1L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(StateActionAdmin.REJECT_EVENT)
                .build();

        EventFullDto expectedDto = EventFullDto.builder().id(eventId).state(EventState.CANCELED).build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(eventRepository.save(pendingEvent)).thenReturn(pendingEvent);
        when(eventStatsCollector.getFullDtoListWithStats(List.of(pendingEvent))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventServiceAdmin.updateEvent(eventId, request);

        assertThat(result).isNotNull();
        assertThat(pendingEvent.getState()).isEqualTo(EventState.CANCELED);
        verify(eventRepository).save(pendingEvent);
    }

    @Test
    @DisplayName("updateEvent: успешная смена категории события")
    void updateEvent_whenCategoryChanged_shouldUpdateCategory() {
        Long eventId = 1L;
        Long newCatId = 20L;
        Category newCategory = new Category(newCatId, "New Category");

        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .categoryId(newCatId)
                .build();

        EventFullDto expectedDto = EventFullDto.builder().id(eventId).build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(categoryRepository.findById(newCatId)).thenReturn(Optional.of(newCategory));
        when(eventRepository.save(pendingEvent)).thenReturn(pendingEvent);
        when(eventStatsCollector.getFullDtoListWithStats(List.of(pendingEvent))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventServiceAdmin.updateEvent(eventId, request);

        assertThat(result).isNotNull();
        assertThat(pendingEvent.getCategory()).isEqualTo(newCategory);
        verify(categoryRepository).findById(newCatId);
    }

    @Test
    @DisplayName("updateEvent: выбрасывает NotFoundException, если событие не найдено")
    void updateEvent_whenEventNotFound_shouldThrowNotFoundException() {
        Long eventId = 999L;
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder().build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventServiceAdmin.updateEvent(eventId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(String.format("Event with id = %d not found", eventId));

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEvent: выбрасывает NotFoundException, если новая категория не найдена")
    void updateEvent_whenCategoryNotFound_shouldThrowNotFoundException() {
        Long eventId = 1L;
        Long nonExistentCatId = 999L;

        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .categoryId(nonExistentCatId)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(categoryRepository.findById(nonExistentCatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventServiceAdmin.updateEvent(eventId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(String.format("Category with id = %d was not found", nonExistentCatId));

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEvent: выбрасывает EventUpdateException при попытке опубликовать событие не в статусе PENDING")
    void updateEvent_whenPublishNotPendingEvent_shouldThrowEventUpdateException() {
        Long eventId = 2L; // Published event
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(StateActionAdmin.PUBLISH_EVENT)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> eventServiceAdmin.updateEvent(eventId, request))
                .isInstanceOf(EventUpdateException.class)
                .hasMessage(String.format("Cannot publish the event because it's not in the PENDING state: %s", EventState.PUBLISHED));

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEvent: выбрасывает EventUpdateException при попытке отменить уже опубликованное событие")
    void updateEvent_whenRejectPublishedEvent_shouldThrowEventUpdateException() {
        Long eventId = 2L; // Published event
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(StateActionAdmin.REJECT_EVENT)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> eventServiceAdmin.updateEvent(eventId, request))
                .isInstanceOf(EventUpdateException.class)
                .hasMessage("Cannot reject the event because it's already PUBLISHED");

        verify(eventRepository, never()).save(any());
    }
}