package com.tenantsphere.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 60) @Pattern(regexp = "^$|^[a-zA-Z0-9_]+$") String username,
        String gender,
        String countryOfOrigin,
        @Size(max = 180) String cityOfOrigin,
        @Size(max = 1000, message = "A bio can be at most 1,000 characters.") String bio,
        String occupation,
        @Size(max = 30) String phoneNumber) {}
