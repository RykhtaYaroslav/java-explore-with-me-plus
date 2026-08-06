package ru.practicum.main.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.model.UserReviewId;

public interface UserReviewRepository extends JpaRepository<UserReview, UserReviewId> {
    @Query("SELECT AVG(ur.score) FROM UserReview ur WHERE ur.target.id = :userId")
    Double findAverageScoreByTargetId(@Param("userId") Long userId);

    boolean existsUserReviewByRaterIdAndEventIdAndTargetId(long raterId, long eventId, long targetId);
}
