package ru.practicum.main.event.service.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.dto.EventPublicParams;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.EventSort;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("getAllWithParams: отправляет hit в статистику и возвращает список DTO")
    void getAllWithParams_shouldSendHitAndReturnEvents() {
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

        List<EventShortDto> result = eventServicePublic.getAllWithParams(defaultParams, "/events", "127.0.0.1");

        assertThat(result)
                .hasSize(2)
                .containsExactly(dto1, dto2);

        // Проверяем отправку статистики с нужными параметрами
        ArgumentCaptor<EndpointHitDto> hitCaptor = ArgumentCaptor.forClass(EndpointHitDto.class);
        verify(statsRepository).hit(hitCaptor.capture());

        EndpointHitDto capturedHit = hitCaptor.getValue();
        assertThat(capturedHit.getApp()).isEqualTo("main-service");
        assertThat(capturedHit.getUri()).isEqualTo("/events");
        assertThat(capturedHit.getIp()).isEqualTo("127.0.0.1");
        assertThat(capturedHit.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("getAllWithParams: если события не найдены, возвращает пустой список без обращения к коллектору")
    void getAllWithParams_whenNoEventsFound_shouldReturnEmptyList() {
        when(eventRepository.findAllWithParams(
                any(), any(), any(), any(), any(), anyBoolean(), any(), anyInt(), anyInt()
        )).thenReturn(Collections.emptyList());

        List<EventShortDto> result = eventServicePublic.getAllWithParams(defaultParams, "/events", "127.0.0.1");

        assertThat(result).isEmpty();

        verify(statsRepository).hit(any(EndpointHitDto.class));
        verify(eventStatsCollector, never()).getShortDtoListWithStats(any());
    }

    @Test
    @DisplayName("getAllWithParams: когда sort = VIEWS, сортирует итоговый список по просмотры в порядке убывания")
    void getAllWithParams_whenSortByViews_shouldSortResultByViewsDesc() {
        EventPublicParams paramsWithViewsSort = new EventPublicParams(
                null, null, null, LocalDateTime.now(), null, false, EventSort.VIEWS, 0, 10
        );

        List<Event> events = List.of(event1, event2);

        // Коллектор изначально вернул в неотсортированном по просмотрам виде (например, 50 просмотров перед 200)
        EventShortDto dtoWithLessViews = EventShortDto.builder().id(1L).views(50L).build();
        EventShortDto dtoWithMoreViews = EventShortDto.builder().id(2L).views(200L).build();

        when(eventRepository.findAllWithParams(
                eq(null), eq(null), eq(null), any(), any(), eq(false), eq("VIEWS"), eq(0), eq(10)
        )).thenReturn(events);

        when(eventStatsCollector.getShortDtoListWithStats(events))
                .thenReturn(List.of(dtoWithLessViews, dtoWithMoreViews));

        List<EventShortDto> result = eventServicePublic.getAllWithParams(paramsWithViewsSort, "/events", "127.0.0.1");

        // Проверяем, что элементы отсортировались по убыванию просмотров (200L, затем 50L)
        assertThat(result)
                .hasSize(2)
                .containsExactly(dtoWithMoreViews, dtoWithLessViews);
    }
}