package ru.practicum.main.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.model.Event;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
            SELECT *
            FROM events
            WHERE initiator_id = :userId
            OFFSET :from
            LIMIT :size
            """, nativeQuery = true)
    List<Event> findByInitiatorId(@Param("userId") Long userId,
                                  @Param("from") Integer from,
                                  @Param("size") Integer size);


}
