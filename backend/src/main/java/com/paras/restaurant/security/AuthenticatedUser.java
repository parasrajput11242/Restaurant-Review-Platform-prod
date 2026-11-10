package com.paras.restaurant.security;

import com.paras.restaurant.domain.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.stream.Collectors;

/** The caller, as described by their Keycloak access token. */
public record AuthenticatedUser(
        String id,
        String username,
        String givenName,
        String familyName,
        String email,
        Set<String> roles
) {

    private static final String ROLE_PREFIX = "ROLE_";

    public static AuthenticatedUser from(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedUser(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"),
                roles
        );
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Roles.ADMIN);
    }

    public User toUser() {
        return User.builder()
                .id(id)
                .username(username)
                .givenName(givenName)
                .familyName(familyName)
                .build();
    }
}
