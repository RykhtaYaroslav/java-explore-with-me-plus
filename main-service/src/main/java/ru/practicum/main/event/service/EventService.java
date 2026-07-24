package ru.practicum.main.event.service;

import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.user.NewEventDto;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);
}
