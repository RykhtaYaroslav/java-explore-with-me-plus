package ru.practicum.main.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.request.model.RequestStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private Long id;
    private String description;
    private LocalDateTime created;

    @JsonProperty("event")
    private Long eventId;

    @JsonProperty("requester")
    private Long requesterId;

    private RequestStatus status;
}
