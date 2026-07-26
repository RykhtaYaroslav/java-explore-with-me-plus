package ru.practicum.main.event.service;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId);

    EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId);
}
