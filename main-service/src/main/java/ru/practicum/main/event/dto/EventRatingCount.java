package ru.practicum.main.event.dto;

import java.math.BigDecimal;

public record EventRatingCount(Long eventId, BigDecimal rating) {
}
