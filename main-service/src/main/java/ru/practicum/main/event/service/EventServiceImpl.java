package ru.practicum.main.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.publicapi.EventFullDto;
import ru.practicum.main.event.dto.user.NewEventDto;
import ru.practicum.main.event.dto.user.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", userId), "UserId", userId));

        Long categoryId = newEventDto.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException((String.format("Category with id = %d not found", categoryId)), "CategoryId", categoryId));

        Event event = eventMapper.toEntity(newEventDto, initiator, category);
        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, 0L, 0L);
    }
}
