package com.paras.restaurant.security;

import java.util.Set;

/**
 * Realm roles defined in Keycloak. Composite roles are configured there:
 * ADMIN includes RESTAURANT_OWNER, which includes USER.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String RESTAURANT_OWNER = "RESTAURANT_OWNER";
    public static final String USER = "USER";

    public static final Set<String> ALL = Set.of(ADMIN, RESTAURANT_OWNER, USER);

    private Roles() {
    }
}
