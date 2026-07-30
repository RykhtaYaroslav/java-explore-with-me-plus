package ru.practicum.main.event.service.publicapi;

import ru.practicum.main.event.dto.EventPublicParams;
import ru.practicum.main.event.dto.EventShortDto;

import java.util.List;

public interface EventServicePublic {
    List<EventShortDto> getAllWithParams(EventPublicParams params, String uri, String ip);
}
