package com.tenantsphere.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RatingCreateRequest(
        @NotBlank(message = "This field may not be blank.") String ratedUserUsername,
        @NotNull(message = "This field is required.")
        @Min(value = 1, message = "\"0\" is not a valid choice.")
        @Max(value = 5, message = "\"6\" is not a valid choice.")
        Integer rating,
        String comment) {}
