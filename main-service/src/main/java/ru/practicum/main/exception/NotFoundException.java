package ru.practicum.main.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {

    private final String fieldName;

    private final transient Object rejectedValue;

    public NotFoundException(String message, String fieldName, Object rejectedValue) {

        super(message);
    }

    public NotFoundException(String message) {

        super(message);
        this.fieldName = null;       // Специфичные поля заполняем заглушками
        this.rejectedValue = null;
    }
}
