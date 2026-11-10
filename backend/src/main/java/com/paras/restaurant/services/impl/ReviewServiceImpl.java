package com.paras.restaurant.services.impl;

import com.paras.restaurant.domain.ReviewCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Photo;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.domain.entities.Review;
import com.paras.restaurant.exceptions.ForbiddenOperationException;
import com.paras.restaurant.exceptions.RestaurantNotFoundException;
import com.paras.restaurant.exceptions.ReviewNotAllowedException;
import com.paras.restaurant.exceptions.ReviewNotFoundException;
import com.paras.restaurant.repositories.RestaurantRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reviews live inside the restaurant document, so every change is a read-modify-write of that
 * document. Restaurant carries an optimistic-locking token: if two people write at the same
 * moment the second save fails with a 409 instead of losing a review.
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final long EDIT_WINDOW_HOURS = 48;

    private final RestaurantRepository restaurantRepository;

    @Override
    public Review createReview(AuthenticatedUser author, String restaurantId, ReviewCreateUpdateRequest request) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        if (restaurant.getCreatedBy() != null && author.id().equals(restaurant.getCreatedBy().getId())) {
            throw new ReviewNotAllowedException("You cannot review your own restaurant");
        }

        boolean alreadyReviewed = restaurant.getReviews().stream()
                .anyMatch(r -> r.getWrittenBy() != null && author.id().equals(r.getWrittenBy().getId()));
        if (alreadyReviewed) {
            throw new ReviewNotAllowedException("You have already reviewed this restaurant");
        }

        LocalDateTime now = now();
        Review review = Review.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent().trim())
                .rating(request.getRating())
                .photos(toPhotos(request.getPhotoIds(), now))
                .datePosted(now)
                .lastEdited(now)
                .writtenBy(author.toUser())
                .build();

        List<Review> reviews = new ArrayList<>(restaurant.getReviews());
        reviews.add(review);
        restaurant.setReviews(reviews);
        updateAverageRating(restaurant);

        restaurantRepository.save(restaurant);
        return review;
    }

    @Override
    public Page<Review> listReviews(String restaurantId, Pageable pageable) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        List<Review> reviews = new ArrayList<>(restaurant.getReviews());

        Comparator<Review> comparator = Comparator.comparing(Review::getDatePosted);
        boolean ascending = false;

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            ascending = order.getDirection().isAscending();
            if ("rating".equals(order.getProperty())) {
                comparator = Comparator.comparing(Review::getRating);
            }
        }
        reviews.sort(ascending ? comparator : comparator.reversed());

        int start = (int) pageable.getOffset();
        if (start >= reviews.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, reviews.size());
        }
        int end = Math.min(start + pageable.getPageSize(), reviews.size());
        return new PageImpl<>(reviews.subList(start, end), pageable, reviews.size());
    }

    @Override
    public Optional<Review> getReview(String restaurantId, String reviewId) {
        return findReview(getRestaurantOrThrow(restaurantId), reviewId);
    }

    @Override
    public Review updateReview(AuthenticatedUser caller, String restaurantId, String reviewId,
                               ReviewCreateUpdateRequest request) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        Review review = findReview(restaurant, reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review does not exist"));

        if (review.getWrittenBy() == null || !caller.id().equals(review.getWrittenBy().getId())) {
            throw new ForbiddenOperationException("You can only edit your own reviews");
        }

        if (now().isAfter(review.getDatePosted().plusHours(EDIT_WINDOW_HOURS))) {
            throw new ReviewNotAllowedException(
                    "Reviews can only be edited within " + EDIT_WINDOW_HOURS + " hours of posting");
        }

        LocalDateTime now = now();
        review.setContent(request.getContent().trim());
        review.setRating(request.getRating());
        review.setLastEdited(now);
        review.setPhotos(toPhotos(request.getPhotoIds(), now));

        updateAverageRating(restaurant);
        restaurantRepository.save(restaurant);
        return review;
    }

    @Override
    public void deleteReview(AuthenticatedUser caller, String restaurantId, String reviewId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        Review review = findReview(restaurant, reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review does not exist"));

        boolean isAuthor = review.getWrittenBy() != null && caller.id().equals(review.getWrittenBy().getId());
        if (!isAuthor && !caller.isAdmin()) {
            throw new ForbiddenOperationException("You can only delete your own reviews");
        }

        List<Review> remaining = new ArrayList<>(restaurant.getReviews());
        remaining.removeIf(r -> reviewId.equals(r.getId()));
        restaurant.setReviews(remaining);

        updateAverageRating(restaurant);
        restaurantRepository.save(restaurant);
    }

    private Restaurant getRestaurantOrThrow(String restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant with id not found: " + restaurantId));
    }

    private static Optional<Review> findReview(Restaurant restaurant, String reviewId) {
        return restaurant.getReviews().stream()
                .filter(r -> reviewId.equals(r.getId()))
                .findFirst();
    }

    private static List<Photo> toPhotos(List<String> photoIds, LocalDateTime uploadedAt) {
        List<Photo> photos = new ArrayList<>();
        if (photoIds != null) {
            for (String photoId : photoIds) {
                photos.add(Photo.builder().url(photoId).uploadDate(uploadedAt).build());
            }
        }
        return photos;
    }

    private static void updateAverageRating(Restaurant restaurant) {
        double average = restaurant.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        restaurant.setAverageRating(Math.round(average * 10) / 10f);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
