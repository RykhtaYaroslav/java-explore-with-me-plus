package ru.practicum.main.review.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;
import ru.practicum.main.review.service.ReviewService;

@RestController
@RequestMapping("/rating")
@AllArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/author/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OutputUserReviewDto createAuthorReview(@Valid @RequestBody NewUserReviewDto review,
                                                  @PathVariable long authorId) {
        return reviewService.createAuthorReview(review, authorId);
    }
}
