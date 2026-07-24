package ru.practicum.main.event;

import org.junit.jupiter.api.BeforeAll;
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
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.LocationDto;
import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.user.NewEventDto;
import ru.practicum.main.event.dto.user.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventServiceImpl;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EventServiceUnitTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @InjectMocks
    private EventServiceImpl eventService;

    private static NewEventDto allFieldsNewEventDto;
    private static NewEventDto minFieldsNewEventDto;

    private static EventFullDto expectedFullDtoFromAllFields;
    private static EventFullDto expectedFullDtoFromMinFields;

    private static final Long USER_ID = 1L;
    private static final User USER = User.builder()
            .id(USER_ID)
            .email("email")
            .name("name")
            .build();

    private static final Long CATEGORY_ID = 10L;
    private static final Category CATEGORY = Category.builder().id(CATEGORY_ID).name("category name").build();

    private static final Long EVENT_ID = 100L;

    @Test
    @DisplayName("Успешное создание нового ивента из DTO со всеми полями")
    void shouldCreateNewEventFromAllFields() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(USER));
        Mockito.when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(CATEGORY));
        Mockito.when(eventRepository.save(Mockito.any(Event.class))).thenAnswer(invocationOnMock -> {
            Event eventToSave = invocationOnMock.getArgument(0);
            eventToSave.setId(EVENT_ID);
            return eventToSave;
        });

        EventFullDto result = eventService.create(USER_ID, allFieldsNewEventDto);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("createdOn")
                .isEqualTo(expectedFullDtoFromAllFields);
    }

    @Test
    @DisplayName("Успешное создание нового ивента из DTO с минимумом информации")
    void shouldCreateNewEventFromMinFields() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(USER));
        Mockito.when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(CATEGORY));
        Mockito.when(eventRepository.save(Mockito.any(Event.class))).thenAnswer(invocationOnMock -> {
            Event eventToSave = invocationOnMock.getArgument(0);
            eventToSave.setId(EVENT_ID);
            return eventToSave;
        });

        EventFullDto result = eventService.create(USER_ID, minFieldsNewEventDto);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("createdOn")
                .isEqualTo(expectedFullDtoFromMinFields);
    }


    @BeforeAll
    static void setUpDto() {
        LocalDateTime now = LocalDateTime.now();

        allFieldsNewEventDto = NewEventDto.builder()
                .title("Заголовок события")
                .annotation("Краткая аннотация для события (минимум 20 символов)")
                .description("Подробное описание события для проверки валидации (минимум 20 символов)")
                .categoryId(CATEGORY_ID)
                .eventDate(now.plusHours(3))
                .location(LocationDto.builder()
                        .lat(55.7558)
                        .lon(37.6173)
                        .build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        minFieldsNewEventDto = NewEventDto.builder()
                .title("Минимальный заголовок")
                .annotation("Аннотация события длиннее 20 символов")
                .description("Описание события длиннее 20 символов для успешной проверки")
                .categoryId(CATEGORY_ID)
                .eventDate(now.plusHours(5))
                .location(new LocationDto(55.7558, 37.6173))
                .build();

        // Ожидаемый результат для allFieldsNewEventDto
        expectedFullDtoFromAllFields = EventFullDto.builder()
                .id(EVENT_ID)
                .title("Заголовок события")
                .annotation("Краткая аннотация для события (минимум 20 символов)")
                .description("Подробное описание события для проверки валидации (минимум 20 символов)")
                .category(CategoryDto.builder()
                        .id(CATEGORY_ID)
                        .name(CATEGORY.getName())
                        .build())
                .createdOn(now)
                .eventDate(now.plusHours(3))
                .publishedOn(null)
                .initiator(UserShortDto.builder()
                        .id(USER_ID)
                        .name(USER.getName())
                        .build())
                .location(LocationDto.builder()
                        .lat(55.7558)
                        .lon(37.6173)
                        .build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .build();

        expectedFullDtoFromMinFields = EventFullDto.builder()
                .id(EVENT_ID)
                .title("Минимальный заголовок")
                .annotation("Аннотация события длиннее 20 символов")
                .description("Описание события длиннее 20 символов для успешной проверки")
                .category(CategoryDto.builder()
                        .id(CATEGORY_ID)
                        .name(CATEGORY.getName())
                        .build())
                .createdOn(now)
                .eventDate(now.plusHours(5))
                .publishedOn(null)
                .initiator(UserShortDto.builder()
                        .id(USER_ID)
                        .name(USER.getName())
                        .build())
                .location(new LocationDto(55.7558, 37.6173))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }
}
