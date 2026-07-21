package ru.practicum.stats.service;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collection;

public interface StatService {
    void saveHit(EndpointHitDto hit);

    Collection<ViewStatsDto> getStats(LocalDateTime startDate,
                                      LocalDateTime endDate,
                                      Collection<String> uris,
                                      boolean unique);
}
