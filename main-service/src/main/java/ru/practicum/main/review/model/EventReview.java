package ru.practicum.main.review.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

import static ru.practicum.main.util.EwmConstants.MAX_REVIEW_COMMENT_LENGTH;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@IdClass(EventReview.class)
@Table(name = "events_reviews")
@Entity
public class EventReview {
    @Id
    @Column(name = "rater_id")
    private Long raterId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comment", length = MAX_REVIEW_COMMENT_LENGTH)
    private String comment;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        EventReview that = (EventReview) o;
        return getRaterId() != null && Objects.equals(getRaterId(), that.getRaterId())
                && getEventId() != null && Objects.equals(getEventId(), that.getEventId())
                && getScore() != null && Objects.equals(getScore(), that.getScore())
                && getComment() != null && Objects.equals(getComment(), that.getComment());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(raterId,
                eventId,
                score,
                comment);
    }
}
