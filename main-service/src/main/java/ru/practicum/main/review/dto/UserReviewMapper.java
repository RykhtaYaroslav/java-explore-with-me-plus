package ru.practicum.main.review.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.review.model.UserReview;

@Mapper(componentModel = "spring")
public interface UserReviewMapper {
    @Mapping(target = "raterId", source = "dto.rater")
    @Mapping(target = "eventId", source = "dto.event")
    @Mapping(target = "targetId", source = "targetId")
    UserReview toUserReview(NewUserReviewDto dto, Long targetId);

    OutputUserReviewDto toOutReviewDto(UserReview review);
}
