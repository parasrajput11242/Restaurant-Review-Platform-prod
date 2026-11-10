package com.paras.restaurant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of every custom {@code app.*} setting in application.properties. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Keycloak keycloak, Storage storage, Geocoding geocoding) {

    public record Keycloak(
            String serverUrl,
            String realm,
            String adminClientId,
            String adminClientSecret
    ) {
    }

    public record Storage(String location) {
    }

    public record Geocoding(String provider, String nominatimUrl, String userAgent) {
    }
}
