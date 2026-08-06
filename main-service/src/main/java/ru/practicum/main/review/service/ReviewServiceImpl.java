package ru.practicum.main.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;
import ru.practicum.main.review.dto.mapper.ReviewMapper;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.repository.EventReviewRepository;
import ru.practicum.main.review.repository.UserReviewRepository;
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
    private final EventStatsCollector eventStatsCollector;

    private final UserReviewRepository userReviewRepository;
    private final EventReviewRepository eventReviewRepository;

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    @Override
    public OutputUserReviewDto createAuthorReview(NewUserReviewDto review, long authorId) {
        Long raterId = review.getRaterId();

        User rater = checkAndReturnUser(raterId);
        User target = checkAndReturnUser(authorId);
        Event event = eventRepository.findById(review.getEventId())
                .orElseThrow(() -> new NotFoundException("Событие с id = " + review.getEventId() + " не найдено"));

        if (event.getInitiator().getId() != authorId) {
            throw new ConflictException("Юзер с id = " + authorId + " не является создателем данного события");
        }

        boolean isParticipant = requestRepository.existsByRequesterIdAndEventIdAndStatus(
                raterId, event.getId(), RequestStatus.CONFIRMED
        );
        if (!isParticipant) {
            throw new ConflictException("Пользователь не является участником данного события и не может оставлять отзыв");
        }

        if (userReviewRepository.existsUserReviewByRaterIdAndEventIdAndTargetId(raterId, event.getId(), authorId)) {
            throw new ConflictException("Данный пользователь уже ставил оценку данному автору на данном мероприятии");
        }

        UserReview rev = reviewMapper.toUserReview(review, rater, target, event);

        rev = userReviewRepository.save(rev);

        return reviewMapper.toUserReviewOutDto(
                rev,
                userMapper.toUserShortDto(rater),
                userMapper.toUserShortDto(target),
                eventStatsCollector.getShortDtoListWithStats(List.of(event)).getFirst()
        );
    }

    private User checkAndReturnUser(long userId) {
        return userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException("Юзер с id = " + userId + " не найден"));
    }

    @Override
    public OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request) {
        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", raterId)));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));


        EventReview eventReview = reviewMapper.toEventReview(request, rater, event);

        eventReview = eventReviewRepository.save(eventReview);

        UserShortDto raterShortDto = userMapper.toUserShortDto(rater);

        EventShortDto eventShortDto = eventStatsCollector.getShortDtoListWithStats(List.of(event)).getFirst();

        return reviewMapper.toEventReviewOutDto(eventReview, raterShortDto, eventShortDto);
    }
}
