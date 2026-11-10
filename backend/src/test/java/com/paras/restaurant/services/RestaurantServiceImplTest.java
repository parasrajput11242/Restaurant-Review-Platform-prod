package com.paras.restaurant.services;

import com.paras.restaurant.domain.GeoLocation;
import com.paras.restaurant.domain.RestaurantCreateUpdateRequest;
import com.paras.restaurant.domain.entities.Address;
import com.paras.restaurant.domain.entities.Restaurant;
import com.paras.restaurant.domain.entities.User;
import com.paras.restaurant.exceptions.ForbiddenOperationException;
import com.paras.restaurant.repositories.RestaurantRepository;
import com.paras.restaurant.repositories.RestaurantSearchRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RestaurantServiceImplTest {

    @Mock RestaurantRepository repository;
    @Mock RestaurantSearchRepository searchRepository;
    @Mock GeoLocationService geo;

    RestaurantServiceImpl service;

    final AuthenticatedUser owner = user("owner-1", "RESTAURANT_OWNER", "USER");
    final AuthenticatedUser otherOwner = user("owner-2", "RESTAURANT_OWNER", "USER");
    final AuthenticatedUser admin = user("admin-1", "ADMIN", "RESTAURANT_OWNER", "USER");

    @BeforeEach
    void setUp() {
        service = new RestaurantServiceImpl(repository, searchRepository, geo);
    }

    private static AuthenticatedUser user(String id, String... roles) {
        return new AuthenticatedUser(id, id, "G", "F", id + "@x.io", Set.of(roles));
    }

    private static RestaurantCreateUpdateRequest request() {
        Address address = Address.builder().streetNumber("1").streetName("Main").city("Mohali")
                .state("PB").postalCode("160055").country("India").build();
        return RestaurantCreateUpdateRequest.builder().name(" Pizza ").cuisineType("Italian")
                .contactInformation("123").address(address).photoIds(List.of("a.png")).build();
    }

    private Restaurant ownedBy(String id) {
        return Restaurant.builder().id("r1").name("Old")
                .createdBy(User.builder().id(id).build()).build();
    }

    @Test
    void createRecordsTheCallerAsOwner() {
        when(geo.geoLocate(any())).thenReturn(new GeoLocation(30.7, 76.7));
        when(repository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

        Restaurant saved = service.createRestaurant(owner, request());

        assertThat(saved.getCreatedBy().getId()).isEqualTo("owner-1");
        assertThat(saved.getName()).isEqualTo("Pizza");
        assertThat(saved.getAverageRating()).isZero();
    }

    @Test
    void ownerCanUpdateOwnRestaurant() {
        Restaurant existing = ownedBy("owner-1");
        when(repository.findById("r1")).thenReturn(Optional.of(existing));
        when(geo.geoLocate(any())).thenReturn(new GeoLocation(1.0, 2.0));
        when(repository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.updateRestaurant(owner, "r1", request()).getName()).isEqualTo("Pizza");
    }

    @Test
    void otherOwnerCannotUpdateOrDelete() {
        when(repository.findById("r1")).thenReturn(Optional.of(ownedBy("owner-1")));

        assertThatThrownBy(() -> service.updateRestaurant(otherOwner, "r1", request()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deleteRestaurant(otherOwner, "r1"))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void adminCanDeleteAnyRestaurant() {
        Restaurant existing = ownedBy("owner-1");
        when(repository.findById("r1")).thenReturn(Optional.of(existing));

        service.deleteRestaurant(admin, "r1");

        verify(repository).delete(existing);
    }
}
