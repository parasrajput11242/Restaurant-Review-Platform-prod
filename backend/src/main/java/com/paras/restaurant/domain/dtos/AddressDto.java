package com.paras.restaurant.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressDto {

    /** Optional: many places have no street number (or use plot / shop / house names). */
    @Size(max = 30, message = "Street number must be at most 30 characters")
    private String streetNumber;

    /** Street, road, sector or area name. */
//    @NotBlank(message = "Street name or area is required")
    @Size(max = 150, message = "Street name must be at most 150 characters")
    private String streetName;

    private String unit;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;
}