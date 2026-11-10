package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.dtos.OwnerRequestCreateDto;
import com.paras.restaurant.domain.dtos.OwnerRequestDto;
import com.paras.restaurant.mappers.OwnerRequestMapper;
import com.paras.restaurant.security.AuthenticatedUser;
import com.paras.restaurant.services.OwnerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/** A regular user asking to become a restaurant owner. */
@RestController
@RequestMapping(path = "/api/owner-requests")
@RequiredArgsConstructor
public class OwnerRequestController {

    private final OwnerRequestService ownerRequestService;
    private final OwnerRequestMapper ownerRequestMapper;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OwnerRequestDto> submit(
            @Valid @RequestBody OwnerRequestCreateDto body,
            JwtAuthenticationToken authentication
    ) {
        var created = ownerRequestService.submit(AuthenticatedUser.from(authentication), body.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerRequestMapper.toDto(created));
    }

    /** 204 when the caller has never applied. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OwnerRequestDto> myRequest(JwtAuthenticationToken authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        return ownerRequestService.findMine(user.id())
                .map(ownerRequestMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
