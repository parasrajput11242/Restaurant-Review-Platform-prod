package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.ReviewCreateUpdateRequest;
import com.paras.restaurant.domain.dtos.PageResponse;
import com.paras.restaurant.domain.dtos.ReviewCreateUpdateRequestDto;
import com.paras.restaurant.domain.dtos.ReviewDto;
import com.paras.restaurant.domain.entities.Review;
import com.paras.restaurant.mappers.ReviewMapper;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/restaurants/{restaurantId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewMapper reviewMapper;
    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewDto> createReview(
            @PathVariable String restaurantId,
            @Valid @RequestBody ReviewCreateUpdateRequestDto body,
            JwtAuthenticationToken authentication
    ) {
        ReviewCreateUpdateRequest request = reviewMapper.toReviewCreateUpdateRequest(body);
        Review created = reviewService.createReview(AuthenticatedUser.from(authentication), restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewMapper.toDto(created));
    }

    /** sort is "datePosted,desc" (default), "datePosted,asc", "rating,desc" or "rating,asc". */
    @GetMapping
    public PageResponse<ReviewDto> listReviews(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "datePosted,desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                reviewService.listReviews(restaurantId, pageRequest(sort, page, size)),
                reviewMapper::toDto);
    }

    @GetMapping(path = "/{reviewId}")
    public ResponseEntity<ReviewDto> getReview(
            @PathVariable String restaurantId,
            @PathVariable String reviewId
    ) {
        return reviewService.getReview(restaurantId, reviewId)
                .map(reviewMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable String restaurantId,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewCreateUpdateRequestDto body,
            JwtAuthenticationToken authentication
    ) {
        ReviewCreateUpdateRequest request = reviewMapper.toReviewCreateUpdateRequest(body);
        Review updated = reviewService.updateReview(
                AuthenticatedUser.from(authentication), restaurantId, reviewId, request);
        return ResponseEntity.ok(reviewMapper.toDto(updated));
    }

    /** Authors delete their own review; admins can delete any review (moderation). */
    @DeleteMapping(path = "/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String restaurantId,
            @PathVariable String reviewId,
            JwtAuthenticationToken authentication
    ) {
        reviewService.deleteReview(AuthenticatedUser.from(authentication), restaurantId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private static PageRequest pageRequest(String sort, int page, int size) {
        String[] parts = sort.split(",");
        String property = "rating".equals(parts[0]) ? "rating" : "datePosted";
        boolean ascending = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(direction, property));
    }
}
