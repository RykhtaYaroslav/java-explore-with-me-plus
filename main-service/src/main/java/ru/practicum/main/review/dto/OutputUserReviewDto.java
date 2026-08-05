package ru.practicum.main.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.user.dto.UserShortDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputUserReviewDto {
    private UserShortDto raterId;
    private UserShortDto targetId;
    private EventShortDto eventId;
    private Integer score;
    private String comment;
}
