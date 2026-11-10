package com.paras.restaurant.services;

import com.paras.restaurant.domain.GeoLocation;
import com.paras.restaurant.domain.entities.Address;

public interface GeoLocationService {
    GeoLocation geoLocate(Address address);
}
