package ru.practicum.main.review.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    EventReview toEventReview(User rater, Event event, NewEventReviewDto request);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "eventReview.score")
    @Mapping(target = "comment", source = "eventReview.comment")
    OutputEventReviewDto toEventReviewDtoOut(EventReview eventReview, UserShortDto rater, EventShortDto event);

}
