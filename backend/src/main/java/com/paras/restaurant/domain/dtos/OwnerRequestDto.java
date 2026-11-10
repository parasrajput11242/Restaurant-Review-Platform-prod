package com.paras.restaurant.domain.dtos;

import com.paras.restaurant.domain.entities.OwnerRequestStatus;

import java.time.LocalDateTime;

public record OwnerRequestDto(
        String id,
        String username,
        String email,
        String message,
        OwnerRequestStatus status,
        LocalDateTime createdAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
}
