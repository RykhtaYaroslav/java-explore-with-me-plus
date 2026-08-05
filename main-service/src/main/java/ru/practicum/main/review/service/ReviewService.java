package ru.practicum.main.review.service;

import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;

public interface ReviewService {
    OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request);
}
