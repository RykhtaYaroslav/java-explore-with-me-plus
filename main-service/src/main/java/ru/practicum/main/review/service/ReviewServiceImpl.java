package ru.practicum.main.review.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;
import ru.practicum.main.review.dto.UserReviewMapper;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.repository.UserReviewRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserReviewMapper mapper;
    private final UserReviewRepository userReviewRepository;
    private final RequestRepository requestRepository;

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

        UserReview rev = mapper.toUserReview(review, rater, target, event);

        return mapper.toOutReviewDto(userReviewRepository.save(rev));
    }

    private User checkAndReturnUser(long userId) {
        return userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException("Юзер с id = " + userId + " не найден"));
    }
}
