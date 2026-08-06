package ru.practicum.main.review.service;

import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;

import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;

public interface ReviewService {
    OutputUserReviewDto createAuthorReview(NewUserReviewDto review, long authorId);

    OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request);
}
