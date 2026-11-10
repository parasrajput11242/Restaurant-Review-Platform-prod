package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.dtos.OwnerRequestDto;
import com.paras.restaurant.domain.dtos.OwnerRequestRejectDto;
import com.paras.restaurant.domain.dtos.PageResponse;
import com.paras.restaurant.domain.entities.OwnerRequestStatus;
import com.paras.restaurant.mappers.OwnerRequestMapper;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.OwnerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/** Admin-only approval queue (also enforced for /api/admin/** in SecurityConfig). */
@RestController
@RequestMapping(path = "/api/admin/owner-requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOwnerRequestController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OwnerRequestService ownerRequestService;
    private final OwnerRequestMapper ownerRequestMapper;

    @GetMapping
    public PageResponse<OwnerRequestDto> list(
            @RequestParam(defaultValue = "PENDING") OwnerRequestStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Oldest first, so the queue is worked in the order people applied.
        var pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        return PageResponse.from(ownerRequestService.list(status, pageable), ownerRequestMapper::toDto);
    }

    @PostMapping("/{id}/approve")
    public OwnerRequestDto approve(@PathVariable String id, JwtAuthenticationToken authentication) {
        return ownerRequestMapper.toDto(
                ownerRequestService.approve(AuthenticatedUser.from(authentication), id));
    }

    @PostMapping("/{id}/reject")
    public OwnerRequestDto reject(
            @PathVariable String id,
            @Valid @RequestBody(required = false) OwnerRequestRejectDto body,
            JwtAuthenticationToken authentication
    ) {
        String reason = body == null ? null : body.reason();
        return ownerRequestMapper.toDto(
                ownerRequestService.reject(AuthenticatedUser.from(authentication), id, reason));
    }
}
