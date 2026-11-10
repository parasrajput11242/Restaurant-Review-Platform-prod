package com.paras.restaurant.services.impl;

import com.paras.restaurant.domain.GeoLocation;
import com.paras.restaurant.domain.entities.Address;
import com.paras.restaurant.services.GeoLocationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Development stand-in that scatters restaurants around London. Enable with
 * {@code app.geocoding.provider=random}. Never use it for real data.
 */
@Service
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "random")
public class RandomLondonGeoLocationService implements GeoLocationService {

    private static final double MIN_LATITUDE = 51.28;
    private static final double MAX_LATITUDE = 51.686;
    private static final double MIN_LONGITUDE = -0.489;
    private static final double MAX_LONGITUDE = 0.236;

    @Override
    public GeoLocation geoLocate(Address address) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return GeoLocation.builder()
                .latitude(random.nextDouble(MIN_LATITUDE, MAX_LATITUDE))
                .longitude(random.nextDouble(MIN_LONGITUDE, MAX_LONGITUDE))
                .build();
    }
}
