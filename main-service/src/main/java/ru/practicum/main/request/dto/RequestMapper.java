package ru.practicum.main.request.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.request.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface RequestMapper {


    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    ParticipationRequestDto toDto(ParticipationRequest request);
}

