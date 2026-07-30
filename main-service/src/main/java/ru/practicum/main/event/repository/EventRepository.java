package ru.practicum.main.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @SuppressWarnings("all")
    @Query(value = """
            SELECT e.*
            FROM events AS e
            WHERE initiator_id = :userId
            ORDER BY e.id ASC
            OFFSET :from
            LIMIT :size
            """, nativeQuery = true)
    List<Event> findByInitiatorId(@Param("userId") Long userId,
                                  @Param("from") Integer from,
                                  @Param("size") Integer size);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Query("select count(e) from Event e where e.category.id = :catId")
    long countEventBySelectCategory(@Param("catId") long catId);

    boolean existsByCategoryId(Long categoryId);

    @SuppressWarnings("all")
    @Query(value = """
            SELECT e.*
            FROM events AS e
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR (LOWER(e.annotation) LIKE LOWER(:text) OR LOWER(e.description) LIKE LOWER(:text)))
            AND (:paid IS NULL OR e.paid = :paid)
            AND (CAST(:rangeStart AS timestamp) IS NULL OR e.event_date >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= :rangeEnd)
            AND (:onlyAvailable = FALSE OR e.participant_limit = 0 OR e.participant_limit > (
                SELECT COUNT(r.id)
                FROM requests AS r
                WHERE r.event_id = e.id AND r.status = 'CONFIRMED'
            ))
            AND (:categories IS NULL OR e.category_id IN (:categories))
            ORDER BY 
                CASE WHEN :sort = 'EVENT_DATE' THEN e.event_date END ASC,
                e.id ASC
            OFFSET :from
            LIMIT :size
            """, nativeQuery = true)
    List<Event> findAllWithParams(@Param("text") String text,
                                  @Param("categories") List<Long> categories,
                                  @Param("paid") Boolean paid,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  @Param("onlyAvailable") Boolean onlyAvailable,
                                  @Param("sort") String sort,
                                  @Param("from") Integer from,
                                  @Param("size") Integer size
    );

    Optional<Event> findByIdAndState(Long id, EventState state);
}
