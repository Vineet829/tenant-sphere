package com.tenantsphere.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RatingCreateRequest(
        @NotBlank(message = "This field may not be blank.") String ratedUserUsername,
        @NotNull(message = "This field is required.")
        @Min(value = 1, message = "\"0\" is not a valid choice.")
        @Max(value = 5, message = "\"6\" is not a valid choice.")
        Integer rating,
        @Size(max = 1000, message = "A comment can be at most 1,000 characters.") String comment) {}
