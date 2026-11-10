package com.paras.restaurant.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private Jwt jwtWith(Map<String, Object> claims) {
        return Jwt.withTokenValue("t").header("alg", "none").subject("u1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claims(c -> c.putAll(claims)).build();
    }

    @Test
    void mapsKnownRealmRolesAndIgnoresOthers() {
        Jwt jwt = jwtWith(Map.of("realm_access", Map.of("roles",
                List.of("ADMIN", "USER", "offline_access", "uma_authorization"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void noRealmAccessClaimMeansNoAuthorities() {
        assertThat(converter.convert(jwtWith(Map.of("scope", "openid")))).isEmpty();
    }
}
