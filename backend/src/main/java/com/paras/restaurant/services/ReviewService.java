package com.paras.restaurant.services;

import com.paras.restaurant.domain.ReviewCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Review;
import com.paras.restaurant.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewService {

    Review createReview(AuthenticatedUser author, String restaurantId, ReviewCreateUpdateRequest review);

    Page<Review> listReviews(String restaurantId, Pageable pageable);

    Optional<Review> getReview(String restaurantId, String reviewId);

    /** Only the author may edit, and only within 48 hours. */
    Review updateReview(AuthenticatedUser caller, String restaurantId, String reviewId, ReviewCreateUpdateRequest review);

    /** The author or an admin (moderation) may delete. */
    void deleteReview(AuthenticatedUser caller, String restaurantId, String reviewId);
}
