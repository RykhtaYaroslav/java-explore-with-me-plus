package ru.practicum.main.event.service.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventPublicParams;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.EventSort;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.util.Comparator;
import java.util.List;

import static java.time.LocalDateTime.now;

@RequiredArgsConstructor
@Service
public class EventServicePublicImpl implements EventServicePublic {
    private final EventRepository eventRepository;
    private final StatsClient statsRepository;

    private final EventStatsCollector eventStatsCollector;


    @Override
    public List<EventShortDto> getAllWithParams(EventPublicParams params) {

        List<Event> events = getEventsWithParamsFromRepository(params);

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> shortDtos = eventStatsCollector.getShortDtoListWithStats(events);

        if (params.sort() == EventSort.VIEWS) {
            shortDtos = shortDtos.stream().sorted(Comparator.comparingLong(EventShortDto::getViews).reversed()).toList();
        }

        return shortDtos;
    }

    @Override
    public EventFullDto getEventFullInformation(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id = %d was not found", id)));

        return eventStatsCollector.getFullDtoListWithStats(List.of(event)).getFirst();
    }

    @Override
    public void hitStat(String uri, String ip) {
        statsRepository.hit(EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri(uri)
                .timestamp(now())
                .build());
    }

    /**
     * Retrieves a list of {@link Event} entities from the repository based on the provided public filtering parameters.
     * <p>
     * Preprocesses input parameters prior to repository query execution:
     * <ul>
     *   <li>Formats search {@code text} with wildcard delimiters ({@code %text%}) for SQL {@code LIKE} filtering, or maps to {@code null} if blank.</li>
     *   <li>Converts empty {@code categories} collections to {@code null} to avoid invalid SQL IN clause execution.</li>
     *   <li>Extracts string representations of the sort option if specified.</li>
     * </ul>
     * </p>
     *
     * @param params the {@link EventPublicParams} record containing request filters, pagination options, and sorting strategy
     * @return a {@link List} of filtered {@link Event} entities matching the specified criteria
     */
    private List<Event> getEventsWithParamsFromRepository(EventPublicParams params) {
        String text = (params.text() == null || params.text().isBlank()) ? null : String.format("%%%s%%", params.text());

        List<Long> categories = CollectionUtils.isEmpty(params.categories()) ? null : params.categories();

        String sort = (params.sort() != null) ? params.sort().name() : null;

        return eventRepository.findAllWithParams(
                text,
                categories,
                params.paid(),
                params.rangeStart(),
                params.rangeEnd(),
                params.onlyAvailable(),
                sort,
                params.from(),
                params.size());
    }


}
