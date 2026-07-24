package ru.practicum.main.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("select count(e) from Event e where e.category.id = :catId")
    long countEventBySelectCategory(@Param("catId") long catId);
}
