package ru.practicum.main.event.dto.privateapi;

public enum StateActionUser {
    SEND_TO_REVIEW, // отправить отменённое событие обратно на модерацию
    CANCEL_REVIEW // отменить модерацию события
}
