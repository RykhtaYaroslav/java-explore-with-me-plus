package ru.practicum.main.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.user.NewEventDto;
import ru.practicum.main.event.service.EventService;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    public EventFullDto create(@RequestBody @Valid NewEventDto newEventDto, @PathVariable Long userId) {
        return eventService.create(userId, newEventDto);
    }
}
