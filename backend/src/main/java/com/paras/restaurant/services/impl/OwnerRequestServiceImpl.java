package com.paras.restaurant.services.impl;

import com.paras.restaurant.client.KeycloakAdminClient;
import com.paras.restaurant.domain.entities.OwnerRequest;
import com.paras.restaurant.domain.entities.OwnerRequestStatus;
import com.paras.restaurant.exceptions.ConflictException;
import com.paras.restaurant.exceptions.OwnerRequestNotFoundException;
import com.paras.restaurant.repositories.OwnerRequestRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.security.Roles;
import com.paras.restaurant.services.OwnerRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerRequestServiceImpl implements OwnerRequestService {

    private final OwnerRequestRepository ownerRequestRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    @Override
    public OwnerRequest submit(AuthenticatedUser user, String message) {
        if (user.hasRole(Roles.RESTAURANT_OWNER)) {
            throw new ConflictException("You already have restaurant owner access");
        }

        Optional<OwnerRequest> existing = ownerRequestRepository.findById(user.id());
        if (existing.isPresent() && existing.get().getStatus() == OwnerRequestStatus.PENDING) {
            throw new ConflictException("You already have a pending request");
        }

        // The id is the Keycloak user id, so this replaces any earlier rejected/approved request.
        OwnerRequest request = OwnerRequest.builder()
                .id(user.id())
                .username(user.username())
                .email(user.email())
                .message(message.trim())
                .status(OwnerRequestStatus.PENDING)
                .createdAt(now())
                .build();

        return ownerRequestRepository.save(request);
    }

    @Override
    public Optional<OwnerRequest> findMine(String userId) {
        return ownerRequestRepository.findById(userId);
    }

    @Override
    public Page<OwnerRequest> list(OwnerRequestStatus status, Pageable pageable) {
        return ownerRequestRepository.findByStatus(status, pageable);
    }

    @Override
    public OwnerRequest approve(AuthenticatedUser admin, String requestId) {
        OwnerRequest request = getOrThrow(requestId);

        if (request.getStatus() == OwnerRequestStatus.APPROVED) {
            return request; // already done, retrying is harmless
        }
        if (request.getStatus() != OwnerRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be approved");
        }

        // Keycloak first: if it fails the request stays PENDING and the admin can simply retry.
        keycloakAdminClient.assignRealmRole(request.getId(), Roles.RESTAURANT_OWNER);

        request.setStatus(OwnerRequestStatus.APPROVED);
        request.setReviewedBy(admin.username());
        request.setReviewedAt(now());
        request.setRejectionReason(null);
        log.info("Owner request of {} approved by {}", request.getUsername(), admin.username());
        return ownerRequestRepository.save(request);
    }

    @Override
    public OwnerRequest reject(AuthenticatedUser admin, String requestId, String reason) {
        OwnerRequest request = getOrThrow(requestId);

        if (request.getStatus() != OwnerRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be rejected");
        }

        request.setStatus(OwnerRequestStatus.REJECTED);
        request.setReviewedBy(admin.username());
        request.setReviewedAt(now());
        request.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        log.info("Owner request of {} rejected by {}", request.getUsername(), admin.username());
        return ownerRequestRepository.save(request);
    }

    private OwnerRequest getOrThrow(String requestId) {
        return ownerRequestRepository.findById(requestId)
                .orElseThrow(() -> new OwnerRequestNotFoundException("Owner request not found"));
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
