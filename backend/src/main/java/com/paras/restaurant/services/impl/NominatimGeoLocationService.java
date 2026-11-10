package com.paras.restaurant.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paras.restaurant.config.AppProperties;
import com.paras.restaurant.domain.GeoLocation;
import com.paras.restaurant.domain.entities.Address;
import com.paras.restaurant.exceptions.AddressNotFoundException;
import com.paras.restaurant.exceptions.UpstreamServiceException;
import com.paras.restaurant.services.GeoLocationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Real geocoding through OpenStreetMap's Nominatim. The public instance allows about one request
 * per second and requires an identifying User-Agent (app.geocoding.user-agent). For heavy traffic
 * run your own Nominatim or swap in another GeoLocationService implementation.
 *
 * Not every place has a street number, and not every street is mapped in detail, so the lookup
 * tries the most precise address first and then falls back to less precise ones:
 * 1) number + street + city..., 2) street + city..., 3) street without postal code, 4) the area
 * (city + postal code). A fallback gives an approximate pin, which is better than no pin.
 */
@Service
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "nominatim", matchIfMissing = true)
public class NominatimGeoLocationService implements GeoLocationService {

    /** Nominatim's usage policy: at most one request per second. */
    private static final long PAUSE_BETWEEN_ATTEMPTS_MS = 1100;

    private final RestClient restClient;

    public NominatimGeoLocationService(AppProperties properties) {
        AppProperties.Geocoding config = properties.geocoding();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = RestClient.builder()
                .baseUrl(config.nominatimUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, config.userAgent())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public GeoLocation geoLocate(Address address) {
        String streetWithNumber = join(" ", address.getStreetNumber(), address.getStreetName());
        String street = join(" ", address.getStreetName());

        List<String> queries = new ArrayList<>();
        addQuery(queries, streetWithNumber, address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());
        addQuery(queries, street, address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());
        addQuery(queries, street, address.getCity(), address.getState(), address.getCountry());
        addQuery(queries, address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());

        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                pause();
            }
            Optional<GeoLocation> found = search(queries.get(i));
            if (found.isPresent()) {
                return found.get();
            }
        }
        throw new AddressNotFoundException("We could not find that address on the map. Please check it.");
    }

    private Optional<GeoLocation> search(String query) {
        List<NominatimResult> results;
        try {
            results = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .queryParam("q", "{q}")
                            .build(query))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NominatimResult>>() {
                    });
        } catch (RestClientException e) {
            throw new UpstreamServiceException("The address lookup service is not available right now", e);
        }

        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        NominatimResult best = results.get(0);
        try {
            return Optional.of(GeoLocation.builder()
                    .latitude(Double.parseDouble(best.lat()))
                    .longitude(Double.parseDouble(best.lon()))
                    .build());
        } catch (NumberFormatException e) {
            throw new UpstreamServiceException("The address lookup service returned an unexpected answer", e);
        }
    }

    private static void addQuery(List<String> queries, String... parts) {
        String query = join(", ", parts);
        if (!query.isBlank() && !queries.contains(query)) {
            queries.add(query);
        }
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(separator));
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_BETWEEN_ATTEMPTS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("The address lookup was interrupted", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimResult(String lat, String lon) {
    }
}