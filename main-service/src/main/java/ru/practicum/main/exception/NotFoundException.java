package ru.practicum.main.exception;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;


@Getter
public class NotFoundException extends RuntimeException {
    private final String fieldName;
    private final transient Object rejectedValue;
    @Autowired
    public NotFoundException(String message, String fieldName, Object rejectedValue) {
        super(message);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
    }

    public NotFoundException(String message) {
        super(message);
        this.fieldName = null;
        this.rejectedValue = null;
    }
}
