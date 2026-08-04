package ru.practicum.main.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.model.EventReviewId;

public interface EventReviewRepository extends JpaRepository<EventReview, EventReviewId> {

}
