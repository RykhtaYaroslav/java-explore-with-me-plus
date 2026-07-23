package ru.practicum.main.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
