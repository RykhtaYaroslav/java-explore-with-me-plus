package ru.practicum.main.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.UpdateEventUserRequest;
import ru.practicum.main.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@RequestBody @Valid NewEventDto newEventDto, @PathVariable @Positive Long userId) {
        return eventService.create(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> findAllByInitiatorId(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventService.findAllByInitiatorId(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findByInitiatorAndEventIds(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
        return eventService.findByInitiatorAndEventIds(userId, eventId);

    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByInitiator(@RequestBody @Valid UpdateEventUserRequest request, @PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
        return eventService.updateEventByInitiator(request, userId, eventId);
    }
}
