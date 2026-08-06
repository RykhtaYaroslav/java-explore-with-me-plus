package ru.practicum.main.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputUserReviewDto {
    private Long rater;
    private Long target;
    private Long eventId;
    private Integer score;
    private String comment;
}
