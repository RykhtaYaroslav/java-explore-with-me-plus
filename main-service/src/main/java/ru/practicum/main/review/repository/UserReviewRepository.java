package ru.practicum.main.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.model.UserReviewId;

public interface UserReviewRepository extends JpaRepository<UserReview, UserReviewId> {
    @Query("SELECT AVG(ur.score) FROM UserReview ur WHERE ur.target.id = :userId")
    Double findAverageScoreByTargetId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(ur) > 0
            FROM UserReview ur
            WHERE ur.rater.id = :raterId
              AND ur.event.id = :eventId
              AND ur.target.id = :targetId
            """)
    boolean existsByParams(
            @Param("raterId") long raterId,
            @Param("eventId") long eventId,
            @Param("targetId") long targetId
    );
}
