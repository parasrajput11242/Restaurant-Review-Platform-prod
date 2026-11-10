package com.paras.restaurant.services;

import com.paras.restaurant.domain.ReviewCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.domain.entities.Review;
import com.paras.restaurant.domain.entities.User;
import com.paras.restaurant.exceptions.ForbiddenOperationException;
import com.paras.restaurant.exceptions.ReviewNotAllowedException;
import com.paras.restaurant.repositories.RestaurantRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock RestaurantRepository repository;
    ReviewServiceImpl service;

    final AuthenticatedUser alice = user("alice", "USER");
    final AuthenticatedUser bob = user("bob", "USER");
    final AuthenticatedUser owner = user("owner", "RESTAURANT_OWNER", "USER");
    final AuthenticatedUser admin = user("admin", "ADMIN", "RESTAURANT_OWNER", "USER");

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(repository);
    }

    private static AuthenticatedUser user(String id, String... roles) {
        return new AuthenticatedUser(id, id, null, null, null, Set.of(roles));
    }

    private static ReviewCreateUpdateRequest body(int rating) {
        return ReviewCreateUpdateRequest.builder().content("Nice").rating(rating).photoIds(List.of()).build();
    }

    private Restaurant restaurantWithReviewBy(String authorId, LocalDateTime posted) {
        Review review = Review.builder().id("rev1").rating(4).content("ok").datePosted(posted)
                .writtenBy(User.builder().id(authorId).build()).build();
        return Restaurant.builder().id("r1").createdBy(User.builder().id("owner").build())
                .reviews(new ArrayList<>(List.of(review))).build();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    @Test
    void ownerCannotReviewOwnRestaurant() {
        when(repository.findById("r1")).thenReturn(Optional.of(restaurantWithReviewBy("alice", now())));

        assertThatThrownBy(() -> service.createReview(owner, "r1", body(5)))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void userCannotReviewTwice() {
        when(repository.findById("r1")).thenReturn(Optional.of(restaurantWithReviewBy("alice", now())));

        assertThatThrownBy(() -> service.createReview(alice, "r1", body(5)))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void createUpdatesAverageRating() {
        Restaurant restaurant = restaurantWithReviewBy("alice", now()); // one 4-star review
        when(repository.findById("r1")).thenReturn(Optional.of(restaurant));

        service.createReview(bob, "r1", body(2));

        assertThat(restaurant.getReviews()).hasSize(2);
        assertThat(restaurant.getAverageRating()).isEqualTo(3.0f);
        verify(repository).save(restaurant);
    }

    @Test
    void onlyTheAuthorCanEdit() {
        when(repository.findById("r1")).thenReturn(Optional.of(restaurantWithReviewBy("alice", now())));

        assertThatThrownBy(() -> service.updateReview(bob, "r1", "rev1", body(1)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.updateReview(admin, "r1", "rev1", body(1)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void editWindowCloses() {
        when(repository.findById("r1")).thenReturn(Optional.of(restaurantWithReviewBy("alice", now().minusHours(49))));

        assertThatThrownBy(() -> service.updateReview(alice, "r1", "rev1", body(1)))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void strangerCannotDeleteButAuthorAndAdminCan() {
        Restaurant restaurant = restaurantWithReviewBy("alice", now());
        when(repository.findById("r1")).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> service.deleteReview(bob, "r1", "rev1"))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());

        service.deleteReview(admin, "r1", "rev1");
        assertThat(restaurant.getReviews()).isEmpty();
        assertThat(restaurant.getAverageRating()).isZero();
    }
}
