package ru.practicum.main.compilation.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
import java.util.stream.Collectors;

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
        Compilation comp;
        if (compilation.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(compilation.getEvents());

            comp = mapper.toCompilation(compilation, events);
        } else {
            comp = mapper.toCompilation(compilation);
        }
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
        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }
        return mapper.toDto(compilationRepository.save(compilation));
    }

    private Compilation checkAndReturnCompilation(long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException("Подборка с id = " + compId + " не найдена!"));
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {

        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {

            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }

        return compilations.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return mapper.toDto(compilation);
    }
}
