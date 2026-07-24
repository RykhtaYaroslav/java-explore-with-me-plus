package ru.practicum.main.event.dto.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.event.dto.LocationDto;
import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.user.NewEventDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.Location;
import ru.practicum.main.user.model.User;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = LocalDateTime.class)
@SuppressWarnings("unused")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "location", source = "newEventDto.location")
    Event toEntity(NewEventDto newEventDto, User initiator, Category category);

    Location toLocation(LocationDto locationDto);

    EventFullDto toFullDto(Event event, Long views, Long confirmedRequests);
}
