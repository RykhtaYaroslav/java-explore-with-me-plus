package ru.practicum.main.event.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventPublicParams(String text,
                                List<Long> categories,
                                Boolean paid,
                                LocalDateTime rangeStart,
                                LocalDateTime rangeEnd,
                                Boolean onlyAvailable,
                                EventSort sort,
                                int from,
                                int size) {
}
