package ru.practicum.stats.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.error.BadRequestException;
import ru.practicum.stats.service.StatService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

@AllArgsConstructor
@RestController
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody EndpointHitDto hit) {
        statService.saveHit(hit);
    }

    @GetMapping("/stats")
    public Collection<ViewStatsDto> getStats(@RequestParam String start,
                                             @RequestParam String end,
                                             @RequestParam(required = false) Collection<String> uris,
                                             @RequestParam(defaultValue = "false") boolean unique) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(start, dtf);
        LocalDateTime endDate = LocalDateTime.parse(end, dtf);

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Начальное время должно быть не позже конца");
        }

        return statService.getStats(startDate, endDate, uris, unique);
    }
}
