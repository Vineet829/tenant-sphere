package com.tenantsphere.profile.dto;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String username,
        String gender,
        String countryOfOrigin,
        String cityOfOrigin,
        String bio,
        String occupation,
        String phoneNumber) {}
