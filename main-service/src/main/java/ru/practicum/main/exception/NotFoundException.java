package ru.practicum.main.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final String fieldName;
    private final transient Object rejectedValue;

    // 1. Продвинутый конструктор (для кода твоего коллеги)
    public NotFoundException(String message, String fieldName, Object rejectedValue) {
        super(message);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
    }

    // 2. Классический конструктор (для твоих методов и категорий)
    public NotFoundException(String message) {
        super(message);
        this.fieldName = null;
        this.rejectedValue = null;
    }
}
