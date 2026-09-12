package com.tenantsphere.apartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApartmentRequest(
        @NotBlank(message = "This field may not be blank.") @Size(max = 10) String unitNumber,
        @NotBlank(message = "This field may not be blank.") @Size(max = 50) String building,
        @NotNull(message = "This field is required.") Integer floor) {}
