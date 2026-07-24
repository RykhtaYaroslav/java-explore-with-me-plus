package ru.practicum.main.compilation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.compilation.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
}