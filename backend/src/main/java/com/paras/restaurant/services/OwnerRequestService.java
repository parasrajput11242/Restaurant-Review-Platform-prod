package com.paras.restaurant.services;

import com.paras.restaurant.domain.entities.OwnerRequest;
import com.paras.restaurant.domain.entities.OwnerRequestStatus;
import com.paras.restaurant.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OwnerRequestService {

    OwnerRequest submit(AuthenticatedUser user, String message);

    Optional<OwnerRequest> findMine(String userId);

    Page<OwnerRequest> list(OwnerRequestStatus status, Pageable pageable);

    /** Grants RESTAURANT_OWNER in Keycloak first, then marks the request approved. Safe to retry. */
    OwnerRequest approve(AuthenticatedUser admin, String requestId);

    OwnerRequest reject(AuthenticatedUser admin, String requestId, String reason);
}
