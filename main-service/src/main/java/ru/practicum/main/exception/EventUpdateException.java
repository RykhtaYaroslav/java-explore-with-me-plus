package ru.practicum.main.exception;

import lombok.Getter;

@Getter
public class EventUpdateException extends RuntimeException {
    private final String fieldName;
    private final transient Object rejectedValue;

    public EventUpdateException(String message, String fieldName, Object rejectedValue) {
        super(message);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;    }
}
