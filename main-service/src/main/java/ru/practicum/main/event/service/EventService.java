package ru.practicum.main.event.service;

import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.publicapi.EventShortDto;
import ru.practicum.main.event.dto.privateapi.NewEventDto;

import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findByInitiatorId(Long userId, Integer from, Integer size);
}
