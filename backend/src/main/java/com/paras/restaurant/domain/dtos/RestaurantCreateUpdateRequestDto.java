package com.paras.restaurant.domain.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantCreateUpdateRequestDto {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 120, message = "Restaurant name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Cuisine type is required")
    @Size(max = 60, message = "Cuisine type must be at most 60 characters")
    private String cuisineType;

    @NotBlank(message = "Contact information is required")
    @Size(max = 120, message = "Contact information must be at most 120 characters")
    private String contactInformation;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    @NotNull(message = "Operating hours are required")
    @Valid
    private OperatingHoursDto operatingHours;

    @NotNull(message = "At least one photo is required")
    @Size(min = 1, max = 10, message = "Between 1 and 10 photos are required")
    private List<@Pattern(regexp = PhotoIds.PATTERN, message = "Invalid photo id") String> photoIds;
}
