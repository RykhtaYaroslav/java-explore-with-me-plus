package ru.practicum.main.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.request.model.ParticipationRequest;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
}
