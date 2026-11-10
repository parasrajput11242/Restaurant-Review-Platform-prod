package com.paras.restaurant.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerRequestCreateDto(
        @NotBlank(message = "Please tell us about your restaurant")
        @Size(min = 10, max = 1000, message = "Message must be between 10 and 1000 characters")
        String message
) {
}
