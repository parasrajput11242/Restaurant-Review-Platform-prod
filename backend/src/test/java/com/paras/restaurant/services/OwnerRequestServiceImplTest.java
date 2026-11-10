package com.paras.restaurant.services;

import com.paras.restaurant.client.KeycloakAdminClient;
import com.paras.restaurant.domain.entities.OwnerRequest;
import com.paras.restaurant.domain.entities.OwnerRequestStatus;
import com.paras.restaurant.exceptions.ConflictException;
import com.paras.restaurant.exceptions.UpstreamServiceException;
import com.paras.restaurant.repositories.OwnerRequestRepository;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.impl.OwnerRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerRequestServiceImplTest {

    @Mock OwnerRequestRepository repository;
    @Mock KeycloakAdminClient keycloak;
    OwnerRequestServiceImpl service;

    final AuthenticatedUser user = new AuthenticatedUser("u1", "sam", null, null, "s@x.io", Set.of("USER"));
    final AuthenticatedUser existingOwner =
            new AuthenticatedUser("u2", "ola", null, null, null, Set.of("RESTAURANT_OWNER", "USER"));
    final AuthenticatedUser admin = new AuthenticatedUser("a1", "root", null, null, null, Set.of("ADMIN"));

    @BeforeEach
    void setUp() {
        service = new OwnerRequestServiceImpl(repository, keycloak);
    }

    private OwnerRequest pending() {
        return OwnerRequest.builder().id("u1").username("sam").status(OwnerRequestStatus.PENDING).build();
    }

    @Test
    void submitCreatesPendingRequestKeyedByUserId() {
        when(repository.findById("u1")).thenReturn(Optional.empty());
        when(repository.save(any(OwnerRequest.class))).thenAnswer(i -> i.getArgument(0));

        OwnerRequest saved = service.submit(user, "  I run a cafe in Mohali  ");

        assertThat(saved.getId()).isEqualTo("u1");
        assertThat(saved.getStatus()).isEqualTo(OwnerRequestStatus.PENDING);
        assertThat(saved.getMessage()).isEqualTo("I run a cafe in Mohali");
    }

    @Test
    void cannotSubmitWhilePendingOrAlreadyOwner() {
        when(repository.findById("u1")).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> service.submit(user, "again please"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.submit(existingOwner, "hello there"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectedUserCanReapply() {
        OwnerRequest rejected = pending();
        rejected.setStatus(OwnerRequestStatus.REJECTED);
        when(repository.findById("u1")).thenReturn(Optional.of(rejected));
        when(repository.save(any(OwnerRequest.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.submit(user, "second attempt!").getStatus()).isEqualTo(OwnerRequestStatus.PENDING);
    }

    @Test
    void approveGrantsRoleThenMarksApproved() {
        when(repository.findById("u1")).thenReturn(Optional.of(pending()));
        when(repository.save(any(OwnerRequest.class))).thenAnswer(i -> i.getArgument(0));

        OwnerRequest result = service.approve(admin, "u1");

        verify(keycloak).assignRealmRole("u1", "RESTAURANT_OWNER");
        assertThat(result.getStatus()).isEqualTo(OwnerRequestStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo("root");
    }

    @Test
    void keycloakFailureLeavesRequestPending() {
        OwnerRequest request = pending();
        when(repository.findById("u1")).thenReturn(Optional.of(request));
        doThrow(new UpstreamServiceException("down")).when(keycloak).assignRealmRole("u1", "RESTAURANT_OWNER");

        assertThatThrownBy(() -> service.approve(admin, "u1")).isInstanceOf(UpstreamServiceException.class);

        assertThat(request.getStatus()).isEqualTo(OwnerRequestStatus.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    void approvingTwiceIsHarmless() {
        OwnerRequest approved = pending();
        approved.setStatus(OwnerRequestStatus.APPROVED);
        when(repository.findById("u1")).thenReturn(Optional.of(approved));

        assertThat(service.approve(admin, "u1").getStatus()).isEqualTo(OwnerRequestStatus.APPROVED);
        verify(keycloak, never()).assignRealmRole(any(), any());
    }

    @Test
    void rejectStoresReason() {
        when(repository.findById("u1")).thenReturn(Optional.of(pending()));
        when(repository.save(any(OwnerRequest.class))).thenAnswer(i -> i.getArgument(0));

        OwnerRequest result = service.reject(admin, "u1", " not enough detail ");

        assertThat(result.getStatus()).isEqualTo(OwnerRequestStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("not enough detail");
        verify(keycloak, never()).assignRealmRole(any(), any());
    }
}
