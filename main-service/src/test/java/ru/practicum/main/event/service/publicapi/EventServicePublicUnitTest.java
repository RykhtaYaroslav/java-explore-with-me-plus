package ru.practicum.main.event.service.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventPublicParams;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.EventSort;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePublicUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StatsClient statsRepository;

    @Mock
    private EventStatsCollector eventStatsCollector;

    @InjectMocks
    private EventServicePublicImpl eventServicePublic;

    private EventPublicParams defaultParams;
    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        defaultParams = new EventPublicParams(
                "search text",
                List.of(1L, 2L),
                true,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5),
                false,
                EventSort.EVENT_DATE,
                0,
                10
        );

        event1 = Event.builder().id(1L).title("Event 1").build();
        event2 = Event.builder().id(2L).title("Event 2").build();
    }

    @Test
    @DisplayName("getAllWithParams: успешный возврат списка DTO без прямой зависимости от HTTP-параметров")
    void getAllWithParams_shouldReturnEvents() {
        List<Event> events = List.of(event1, event2);
        EventShortDto dto1 = EventShortDto.builder().id(1L).views(100L).build();
        EventShortDto dto2 = EventShortDto.builder().id(2L).views(50L).build();

        when(eventRepository.findAllWithParams(
                eq("%search text%"),
                eq(List.of(1L, 2L)),
                eq(true),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(false),
                eq("EVENT_DATE"),
                eq(0),
                eq(10)
        )).thenReturn(events);

        when(eventStatsCollector.getShortDtoListWithStats(events)).thenReturn(List.of(dto1, dto2));

        List<EventShortDto> result = eventServicePublic.getAllWithParams(defaultParams);

        assertThat(result)
                .hasSize(2)
                .containsExactly(dto1, dto2);

        verify(eventStatsCollector).getShortDtoListWithStats(events);
    }

    @Test
    @DisplayName("getAllWithParams: если события не найдены, возвращает пустой список без обращения к коллектору")
    void getAllWithParams_whenNoEventsFound_shouldReturnEmptyList() {
        when(eventRepository.findAllWithParams(
                any(), any(), any(), any(), any(), anyBoolean(), any(), anyInt(), anyInt()
        )).thenReturn(Collections.emptyList());

        List<EventShortDto> result = eventServicePublic.getAllWithParams(defaultParams);

        assertThat(result).isEmpty();
        verify(eventStatsCollector, never()).getShortDtoListWithStats(any());
    }

    @Test
    @DisplayName("getAllWithParams: когда sort = VIEWS, сортирует итоговый список по просмотрам в порядке убывания")
    void getAllWithParams_whenSortByViews_shouldSortResultByViewsDesc() {
        EventPublicParams paramsWithViewsSort = new EventPublicParams(
                null, null, null, LocalDateTime.now(), null, false, EventSort.VIEWS, 0, 10
        );

        List<Event> events = List.of(event1, event2);

        EventShortDto dtoWithLessViews = EventShortDto.builder().id(1L).views(50L).build();
        EventShortDto dtoWithMoreViews = EventShortDto.builder().id(2L).views(200L).build();

        when(eventRepository.findAllWithParams(
                eq(null), eq(null), eq(null), any(), any(), eq(false), eq("VIEWS"), eq(0), eq(10)
        )).thenReturn(events);

        when(eventStatsCollector.getShortDtoListWithStats(events))
                .thenReturn(List.of(dtoWithLessViews, dtoWithMoreViews));

        List<EventShortDto> result = eventServicePublic.getAllWithParams(paramsWithViewsSort);

        assertThat(result)
                .hasSize(2)
                .containsExactly(dtoWithMoreViews, dtoWithLessViews);
    }

    @Test
    @DisplayName("getEventFullInformation: успешное получение полной информации по ID события")
    void getEventFullInformation_shouldReturnEventFullDto() {
        Long eventId = 1L;
        EventFullDto expectedDto = EventFullDto.builder()
                .id(eventId)
                .title("Event 1")
                .category(CategoryDto.builder().id(10L).build())
                .initiator(UserShortDto.builder().id(100L).build())
                .views(10L)
                .confirmedRequests(5L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event1));
        when(eventStatsCollector.getFullDtoListWithStats(List.of(event1))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventServicePublic.getEventFullInformation(eventId);

        assertThat(result).isNotNull().isEqualTo(expectedDto);
        verify(eventRepository).findById(eventId);
        verify(eventStatsCollector).getFullDtoListWithStats(List.of(event1));
    }

    @Test
    @DisplayName("getEventFullInformation: выбрасывает NotFoundException, если событие не найдено")
    void getEventFullInformation_whenNotFound_shouldThrowNotFoundException() {
        Long eventId = 999L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventServicePublic.getEventFullInformation(eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(String.format("Event with id = %d was not found", eventId));

        verify(eventRepository).findById(eventId);
        verify(eventStatsCollector, never()).getFullDtoListWithStats(any());
    }

    @Test
    @DisplayName("hitStat: корректно формирует и отправляет DTO хита в сервисе статистики")
    void hitStat_shouldSendEndpointHitDtoToStatsRepository() {
        String uri = "/events/1";
        String ip = "192.168.0.1";

        eventServicePublic.hitStat(uri, ip);

        ArgumentCaptor<EndpointHitDto> hitCaptor = ArgumentCaptor.forClass(EndpointHitDto.class);
        verify(statsRepository).hit(hitCaptor.capture());

        EndpointHitDto capturedHit = hitCaptor.getValue();
        assertThat(capturedHit.getApp()).isEqualTo("main-service");
        assertThat(capturedHit.getUri()).isEqualTo(uri);
        assertThat(capturedHit.getIp()).isEqualTo(ip);
        assertThat(capturedHit.getTimestamp()).isNotNull();
    }
}