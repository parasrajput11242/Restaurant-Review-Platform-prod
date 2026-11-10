package com.paras.restaurant.services.impl;

import com.paras.restaurant.domain.GeoLocation;
import com.paras.restaurant.domain.RestaurantCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Photo;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.exceptions.ForbiddenOperationException;
import com.paras.restaurant.exceptions.RestaurantNotFoundException;
import com.paras.restaurant.repositories.RestaurantRepository;
import com.paras.restaurant.repositories.RestaurantSearchRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.GeoLocationService;
import com.paras.restaurant.services.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantSearchRepository restaurantSearchRepository;
    private final GeoLocationService geoLocationService;

    @Override
    public Restaurant createRestaurant(AuthenticatedUser owner, RestaurantCreateUpdateRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName().trim())
                .cuisineType(request.getCuisineType().trim())
                .contactInformation(request.getContactInformation().trim())
                .address(request.getAddress())
                .geoLocation(locate(request))
                .operatingHours(request.getOperatingHours())
                .averageRating(0f)
                .photos(toPhotos(request.getPhotoIds()))
                .createdBy(owner.toUser())
                .build();

        return restaurantRepository.save(restaurant);
    }

    @Override
    public Page<Restaurant> searchRestaurants(
            String query, Float minRating, Float latitude,
            Float longitude, Float radius, Pageable pageable) {
        return restaurantSearchRepository.search(query, minRating, latitude, longitude, radius, pageable);
    }

    @Override
    public Page<Restaurant> listOwnedBy(String ownerId, Pageable pageable) {
        return restaurantSearchRepository.findByOwner(ownerId, pageable);
    }

    @Override
    public Optional<Restaurant> getRestaurant(String id) {
        return restaurantRepository.findById(id);
    }

    @Override
    public Restaurant updateRestaurant(AuthenticatedUser caller, String id, RestaurantCreateUpdateRequest request) {
        Restaurant restaurant = getRestaurantOrThrow(id);
        requireOwnerOrAdmin(caller, restaurant);

        restaurant.setName(request.getName().trim());
        restaurant.setCuisineType(request.getCuisineType().trim());
        restaurant.setContactInformation(request.getContactInformation().trim());
        restaurant.setOperatingHours(request.getOperatingHours());
        restaurant.setPhotos(toPhotos(request.getPhotoIds()));

        // Geocoding is a network call, so only repeat it when the address really changed.
        if (!request.getAddress().equals(restaurant.getAddress())) {
            restaurant.setAddress(request.getAddress());
            restaurant.setGeoLocation(locate(request));
        }

        return restaurantRepository.save(restaurant);
    }

    @Override
    public void deleteRestaurant(AuthenticatedUser caller, String id) {
        Restaurant restaurant = getRestaurantOrThrow(id);
        requireOwnerOrAdmin(caller, restaurant);
        restaurantRepository.delete(restaurant);
    }

    private Restaurant getRestaurantOrThrow(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant with ID does not exist: " + id));
    }

    private static void requireOwnerOrAdmin(AuthenticatedUser caller, Restaurant restaurant) {
        boolean isOwner = restaurant.getCreatedBy() != null
                && caller.id().equals(restaurant.getCreatedBy().getId());
        if (!isOwner && !caller.isAdmin()) {
            throw new ForbiddenOperationException("You can only change your own restaurants");
        }
    }

    private GeoPoint locate(RestaurantCreateUpdateRequest request) {
        GeoLocation location = geoLocationService.geoLocate(request.getAddress());
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }

    private static List<Photo> toPhotos(List<String> photoIds) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Photo> photos = new ArrayList<>();
        for (String photoId : photoIds) {
            photos.add(Photo.builder().url(photoId).uploadDate(now).build());
        }
        return photos;
    }
}
