package com.paras.restaurant.mappers;

import com.paras.restaurant.domain.ReviewCreateUpdateRequest;
import com.paras.restaurant.domain.dtos.ReviewCreateUpdateRequestDto;
import com.paras.restaurant.domain.dtos.ReviewDto;
import com.paras.restaurant.domain.entities.Review;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    ReviewCreateUpdateRequest toReviewCreateUpdateRequest(ReviewCreateUpdateRequestDto dto);

    ReviewDto toDto(Review review);

}
