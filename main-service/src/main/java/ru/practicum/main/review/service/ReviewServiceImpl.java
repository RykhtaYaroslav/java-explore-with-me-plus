package ru.practicum.main.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.dto.mapper.ReviewMapper;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.repository.EventReviewRepository;
import ru.practicum.main.user.dto.UserMapper;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventReviewRepository eventReviewRepository;

    private final EventStatsCollector eventStatsCollector;

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    @Override
    public OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request) {
        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", raterId)));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));


        EventReview eventReview = reviewMapper.toEventReview(rater, event, request);

        eventReview = eventReviewRepository.save(eventReview);

        UserShortDto raterShortDto = userMapper.toUserShortDto(rater);

        EventShortDto eventShortDto = eventStatsCollector.getShortDtoListWithStats(List.of(event)).getFirst();

        return reviewMapper.toEventReviewDtoOut(eventReview, raterShortDto, eventShortDto);
    }
}
