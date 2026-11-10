package com.paras.restaurant.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Turns Keycloak's {@code realm_access.roles} claim into Spring authorities
 * ({@code ADMIN} becomes {@code ROLE_ADMIN}). Roles we do not know about
 * (offline_access, uma_authorization, ...) are ignored.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }
        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return List.of();
        }
        return roleCollection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(Roles.ALL::contains)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
