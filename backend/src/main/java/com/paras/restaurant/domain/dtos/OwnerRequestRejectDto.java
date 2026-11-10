package com.paras.restaurant.domain.dtos;

import jakarta.validation.constraints.Size;

public record OwnerRequestRejectDto(
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
