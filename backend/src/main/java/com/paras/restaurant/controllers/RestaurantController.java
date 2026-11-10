package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.RestaurantCreateUpdateRequest;
import com.paras.restaurant.domain.dtos.PageResponse;
import com.paras.restaurant.domain.dtos.RestaurantCreateUpdateRequestDto;
import com.paras.restaurant.domain.dtos.RestaurantDto;
import com.paras.restaurant.domain.dtos.RestaurantSummaryDto;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.mappers.RestaurantMapper;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private static final int MAX_PAGE_SIZE = 50;

    private final RestaurantService restaurantService;
    private final RestaurantMapper restaurantMapper;

    /** Owners (and admins, who include the owner role) can add restaurants. */
    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantDto> createRestaurant(
            @Valid @RequestBody RestaurantCreateUpdateRequestDto request,
            JwtAuthenticationToken authentication
    ) {
        RestaurantCreateUpdateRequest createRequest = restaurantMapper.toRestaurantCreateUpdateRequest(request);
        Restaurant restaurant = restaurantService.createRestaurant(AuthenticatedUser.from(authentication), createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantMapper.toRestaurantDto(restaurant));
    }

    @GetMapping
    public PageResponse<RestaurantSummaryDto> searchRestaurants(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) Float latitude,
            @RequestParam(required = false) Float longitude,
            @RequestParam(required = false) Float radius,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Restaurant> results = restaurantService.searchRestaurants(
                q, minRating, latitude, longitude, radius, pageRequest(page, size));
        return PageResponse.from(results, restaurantMapper::toSummaryDto);
    }

    /** The caller's own restaurants. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public PageResponse<RestaurantSummaryDto> myRestaurants(
            JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        Page<Restaurant> results = restaurantService.listOwnedBy(user.id(), pageRequest(page, size));
        return PageResponse.from(results, restaurantMapper::toSummaryDto);
    }

    @GetMapping(path = "/{restaurantId}")
    public ResponseEntity<RestaurantDto> getRestaurant(@PathVariable String restaurantId) {
        return restaurantService.getRestaurant(restaurantId)
                .map(restaurant -> ResponseEntity.ok(restaurantMapper.toRestaurantDto(restaurant)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantDto> updateRestaurant(
            @PathVariable String restaurantId,
            @Valid @RequestBody RestaurantCreateUpdateRequestDto requestDto,
            JwtAuthenticationToken authentication
    ) {
        RestaurantCreateUpdateRequest request = restaurantMapper.toRestaurantCreateUpdateRequest(requestDto);
        Restaurant updated = restaurantService.updateRestaurant(
                AuthenticatedUser.from(authentication), restaurantId, request);
        return ResponseEntity.ok(restaurantMapper.toRestaurantDto(updated));
    }

    @DeleteMapping(path = "/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable String restaurantId,
            JwtAuthenticationToken authentication
    ) {
        restaurantService.deleteRestaurant(AuthenticatedUser.from(authentication), restaurantId);
        return ResponseEntity.noContent().build();
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
