package ru.practicum.main.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.model.UserReviewId;

public interface UserReviewRepository extends JpaRepository<UserReview, UserReviewId> {
}
