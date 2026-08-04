package ru.practicum.main.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewUserReviewDto {
    @NotNull
    private Long rater;

    @NotNull
    private Long event;

    @NotNull
    @Range(min = 1, max = 5, message = "Оценка должна быть в диапазоне от 1 до 5")
    private int score;

    @NotBlank
    private String comment;
}
