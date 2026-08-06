package ru.practicum.main.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.user.dto.UserShortDto;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutputUserReviewDto {
    private UserShortDto rater;
    private UserShortDto target;
    private EventShortDto event;
    private int score;
    private String comment;
}
