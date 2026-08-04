package ru.practicum.main.review.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.review.model.UserReview;

@Mapper(componentModel = "spring")
public interface UserReviewMapper {
    @Mapping(target = "raterId", source = "rater")
    @Mapping(target = "eventId", source = "event")
    @Mapping(target = "targetId", ignore = true)
    UserReview toUserReview(NewUserReviewDto dto);

    OutputUserReviewDto toOutReviewDto(UserReview review);
}
