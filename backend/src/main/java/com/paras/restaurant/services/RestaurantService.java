package com.paras.restaurant.services;

import com.paras.restaurant.domain.RestaurantCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RestaurantService {

    /** The caller becomes the restaurant's owner. */
    Restaurant createRestaurant(AuthenticatedUser owner, RestaurantCreateUpdateRequest request);

    Page<Restaurant> searchRestaurants(
            String query,
            Float minRating,
            Float latitude,
            Float longitude,
            Float radius,
            Pageable pageable
    );

    Page<Restaurant> listOwnedBy(String ownerId, Pageable pageable);

    Optional<Restaurant> getRestaurant(String id);

    /** Only the restaurant's owner or an admin may update. */
    Restaurant updateRestaurant(AuthenticatedUser caller, String id, RestaurantCreateUpdateRequest request);

    /** Only the restaurant's owner or an admin may delete. */
    void deleteRestaurant(AuthenticatedUser caller, String id);
}
