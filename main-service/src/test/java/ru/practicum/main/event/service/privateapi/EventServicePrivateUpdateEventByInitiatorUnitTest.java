package ru.practicum.main.event.service.privateapi;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
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
import ru.practicum.main.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateUpdateEventByInitiatorUnitTest {

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
    private EventServicePrivateImpl eventService;

    private EasyRandom easyRandom;
    private User user;
    private Event event;
    private Category category;

    private static final Long USER_ID = 100L;
    private static final Long EVENT_ID = 1L;
    private static final Long NEW_CATEGORY_ID = 5L;
    private static final Long VIEWS = 999L;
    private static final Long CONFIRMED_REQUESTS = 9999L;

    @BeforeEach
    void setUp() {
        easyRandom = TestDataUtils.createEventEasyRandomizer();

        user = easyRandom.nextObject(User.class);
        user.setId(USER_ID);

        category = easyRandom.nextObject(Category.class);
        category.setId(NEW_CATEGORY_ID);

        event = easyRandom.nextObject(Event.class);
        event.setId(EVENT_ID);
        event.setInitiator(user);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now().minusDays(1));
        event.setEventDate(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("Успешное обновление всех полей ивента через EasyRandom DTO")
    void shouldUpdateAllEventFields() {
        UpdateEventUserRequest request = easyRandom.nextObject(UpdateEventUserRequest.class);
        request.setEventDate(LocalDateTime.now().plusDays(5));
        request.setCategoryId(NEW_CATEGORY_ID);
        request.setStateAction(StateActionUser.CANCEL_REVIEW);

        mockCommonValidations();
        mockStatsAndRequests();
        when(categoryRepository.findById(NEW_CATEGORY_ID)).thenReturn(Optional.of(category));

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(request.getTitle(), result.getTitle()),
                () -> assertEquals(request.getAnnotation(), result.getAnnotation()),
                () -> assertEquals(request.getDescription(), result.getDescription()),
                () -> assertEquals(NEW_CATEGORY_ID, result.getCategory().getId()),
                () -> assertEquals(EventState.CANCELED, result.getState()),
                () -> assertEquals(VIEWS, result.getViews()),
                () -> assertEquals(CONFIRMED_REQUESTS, result.getConfirmedRequests())
        );

        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Не должно изменять поля, если передано DTO со всеми null")
    void shouldNotChangeAnythingWhenAllRequestFieldsAreNull() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();

        String originalTitle = event.getTitle();
        String originalDescription = event.getDescription();
        EventState originalState = event.getState();

        mockCommonValidations();
        mockStatsAndRequests();

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertAll(
                () -> assertEquals(originalTitle, result.getTitle()),
                () -> assertEquals(originalDescription, result.getDescription()),
                () -> assertEquals(originalState, result.getState()),
                () -> assertEquals(VIEWS, result.getViews()),
                () -> assertEquals(CONFIRMED_REQUESTS, result.getConfirmedRequests())
        );
    }

    @Test
    @DisplayName("Частичное обновление: только title, остальные поля не затираются")
    void shouldUpdateOnlyTitleWithoutOverwritingOtherFields() {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .title("New Unique Title")
                .build();

        String originalAnnotation = event.getAnnotation();
        String originalDescription = event.getDescription();

        mockCommonValidations();
        mockStatsAndRequests();

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertAll(
                () -> assertEquals("New Unique Title", result.getTitle()),
                () -> assertEquals(originalAnnotation, result.getAnnotation()),
                () -> assertEquals(originalDescription, result.getDescription())
        );
    }

    @Test
    @DisplayName("Обновление категории: успешный поиск и присвоение новой категории")
    void shouldUpdateCategoryWhenCategoryIdIsProvided() {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .categoryId(NEW_CATEGORY_ID)
                .build();

        mockCommonValidations();
        mockStatsAndRequests();
        when(categoryRepository.findById(NEW_CATEGORY_ID)).thenReturn(Optional.of(category));

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertEquals(NEW_CATEGORY_ID, result.getCategory().getId());
        verify(categoryRepository).findById(NEW_CATEGORY_ID);
    }

    @Test
    @DisplayName("Смена статуса на PENDING при action = SEND_TO_REVIEW")
    void shouldSetStateToPendingWhenSendToReview() {
        event.setState(EventState.CANCELED);
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(StateActionUser.SEND_TO_REVIEW)
                .build();

        mockCommonValidations();
        mockStatsAndRequests();

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertEquals(EventState.PENDING, result.getState());
    }

    @Test
    @DisplayName("Успешное обновление даты, если старая дата < 2 часов, но передана новая валидная дата")
    void shouldAllowUpdateWhenOldDateExpiredButNewDateIsValid() {
        event.setEventDate(LocalDateTime.now().plusHours(1)); // Старая дата поджала (< 2 часов)
        LocalDateTime validNewDate = LocalDateTime.now().plusDays(3);

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .eventDate(validNewDate)
                .build();

        mockCommonValidations();
        mockStatsAndRequests();

        EventFullDto result = eventService.updateEventByInitiator(request, USER_ID, EVENT_ID);

        assertEquals(validNewDate, result.getEventDate());
    }

    @Test
    @DisplayName("Исключение NotFoundException, если пользователь не найден")
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.updateEventByInitiator(request, USER_ID, EVENT_ID));

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Исключение NotFoundException, если событие не найдено или не принадлежит пользователю")
    void shouldThrowNotFoundExceptionWhenEventDoesNotExistOrNotOwned() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> eventService.updateEventByInitiator(request, USER_ID, EVENT_ID)
        );

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Исключение EventUpdateException, если событие уже опубликовано (PUBLISHED)")
    void shouldThrowEventUpdateExceptionWhenEventIsPublished() {
        event.setState(EventState.PUBLISHED);
        UpdateEventUserRequest request = new UpdateEventUserRequest();

        mockCommonValidations();

        assertThrows(
                EventUpdateException.class,
                () -> eventService.updateEventByInitiator(request, USER_ID, EVENT_ID)
        );

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Исключение EventUpdateException, если старая дата < 2 часов и в DTO не передали новую дату")
    void shouldThrowEventUpdateExceptionWhenOldDateIsWithinTwoHoursAndNoNewDate() {
        LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
        event.setEventDate(expiredDate);
        UpdateEventUserRequest request = new UpdateEventUserRequest(); // new date is null

        mockCommonValidations();

        assertThrows(
                EventUpdateException.class,
                () -> eventService.updateEventByInitiator(request, USER_ID, EVENT_ID)
        );

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Исключение NotFoundException, если передан несуществующий categoryId")
    void shouldThrowNotFoundExceptionWhenCategoryDoesNotExist() {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .categoryId(NEW_CATEGORY_ID)
                .build();

        mockCommonValidations();
        when(categoryRepository.findById(NEW_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> eventService.updateEventByInitiator(request, USER_ID, EVENT_ID)
        );

        verify(eventRepository, never()).save(any());
    }

    private void mockCommonValidations() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
    }

    private void mockStatsAndRequests() {
        List<Long> eventIds = List.of(EVENT_ID);

        when(statRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), any(), anyBoolean()))
                .thenReturn(getViewStatsDto(eventIds, VIEWS));
        when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(List.of(new ConfirmedRequestsCount(EVENT_ID, CONFIRMED_REQUESTS)));
    }

    private List<ViewStatsDto> getViewStatsDto(List<Long> eventIds, Long views) {
        return eventIds.stream()
                .map(id -> String.format("/events/%d", id))
                .map(uri -> new ViewStatsDto("ewm", uri, views))
                .toList();
    }
}