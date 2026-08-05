package ru.practicum.main.review.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.service.ReviewService;

@RestController
@RequestMapping("/rating")
@RequiredArgsConstructor
public class RatingController {
    private final ReviewService reviewService;

    @PostMapping("{raterId}/event/{eventId}")
    public OutputEventReviewDto createEventReview(@PathVariable @Positive(message = "Rater id must be positive") Long raterId,
                                                  @PathVariable @Positive(message = "Event id must be positive") Long eventId,
                                                  @RequestBody @Valid NewEventReviewDto request) {
        return reviewService.createEventReview(raterId, eventId, request);
    }

}
// @Positive(message = "Target user id must be positive")