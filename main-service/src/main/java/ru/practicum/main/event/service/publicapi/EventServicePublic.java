package ru.practicum.main.event.service.publicapi;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventPublicParams;
import ru.practicum.main.event.dto.EventShortDto;

import java.util.List;

public interface EventServicePublic {
    List<EventShortDto> getAllWithParams(EventPublicParams params);

    EventFullDto getEventFullInformation(Long id);

    void hitStat(String uri, String ip);
}
