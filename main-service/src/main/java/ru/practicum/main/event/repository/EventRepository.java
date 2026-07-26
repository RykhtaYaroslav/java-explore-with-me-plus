package ru.practicum.main.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.model.Event;

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
}
