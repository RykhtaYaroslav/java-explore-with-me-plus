package ru.practicum.main.event.service.adminapi;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventQueryParams;

import java.util.List;

public interface EventServiceAdmin {
    List<EventFullDto> getAllFullInfoWithParams(EventQueryParams params);
}
