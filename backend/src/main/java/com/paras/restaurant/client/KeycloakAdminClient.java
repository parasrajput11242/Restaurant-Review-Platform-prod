package com.paras.restaurant.client;

/** The small slice of the Keycloak Admin API this application needs. */
public interface KeycloakAdminClient {

    /** Gives a user a realm role. Granting a role the user already has is not an error. */
    void assignRealmRole(String userId, String roleName);
}
