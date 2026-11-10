package com.paras.restaurant.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paras.restaurant.config.AppProperties;
import com.paras.restaurant.exceptions.UpstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Talks to Keycloak as the "restaurant-review-backend" service account (client-credentials grant).
 * That account has only the realm-management permissions needed to look up and map a realm role.
 */
@Component
@Slf4j
public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    private final AppProperties.Keycloak config;
    private final RestClient restClient;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    public KeycloakAdminClientImpl(AppProperties properties) {
        this.config = properties.keycloak();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(stripTrailingSlash(config.serverUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void assignRealmRole(String userId, String roleName) {
        try {
            String token = accessToken();

            RoleRepresentation role = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{role}", config.realm(), roleName)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .body(RoleRepresentation.class);
            if (role == null || role.id() == null) {
                throw new UpstreamServiceException("Keycloak realm role '" + roleName + "' was not found");
            }

            restClient.post()
                    .uri("/admin/realms/{realm}/users/{user}/role-mappings/realm", config.realm(), userId)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Granted realm role {} to user {}", roleName, userId);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Could not update the user's role in Keycloak", e);
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", config.adminClientId());
        form.add("client_secret", config.adminClientSecret());

        TokenResponse response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", config.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new UpstreamServiceException("Keycloak did not return an access token");
        }

        cachedToken = response.accessToken();
        // Renew a little early so we never send a token that expires in flight.
        cachedTokenExpiry = Instant.now().plusSeconds(Math.max(response.expiresIn() - 15, 5));
        return cachedToken;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoleRepresentation(String id, String name) {
    }
}
