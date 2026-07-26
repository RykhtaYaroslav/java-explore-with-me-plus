package ru.practicum.main.event.service;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;

import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId);
}
