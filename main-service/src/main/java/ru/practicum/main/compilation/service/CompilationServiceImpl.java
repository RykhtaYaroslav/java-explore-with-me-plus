package ru.practicum.main.compilation.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.CompilationMapper;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

@AllArgsConstructor
@Service
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Override
    public CompilationDto createCompilation(NewCompilationDto compilation) {
        if (compilationRepository.existsByTitle(compilation.getTitle())) {
            throw new ConflictException("Подборка с заголовком = " + compilation.getTitle() + " уже существует");
        }

        List<Event> events = eventRepository.findAllById(compilation.getEvents());

        Compilation comp = mapper.toCompilation(compilation, events);
        return mapper.toDto(compilationRepository.save(comp));
    }

    @Override
    public void deleteCompilation(long compId) {
        checkAndReturnCompilation(compId);

        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest update) {
        Compilation compilation = checkAndReturnCompilation(compId);
        if (update.getTitle() != null
                && !update.getTitle().equals(compilation.getTitle())
                && compilationRepository.existsByTitle(update.getTitle())) {
            throw new ConflictException("Подборка с заголовком = " + update.getTitle() + " уже существует");
        }
        if (update.getTitle() != null) {
            compilation.setTitle(update.getTitle());
        }
        if (update.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(update.getEvents());
            compilation.setEvents(events);
        }
        compilation.setPinned(update.isPinned());
        return mapper.toDto(compilationRepository.save(compilation));
    }

    private Compilation checkAndReturnCompilation(long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException("Подборка с id = " + compId + " не найдена!"));
    }
}
