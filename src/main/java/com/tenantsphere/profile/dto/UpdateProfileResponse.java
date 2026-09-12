package com.tenantsphere.profile.dto;

import com.tenantsphere.common.Countries;
import com.tenantsphere.profile.Profile;

public record UpdateProfileResponse(
        String firstName,
        String lastName,
        String username,
        String gender,
        String countryOfOrigin,
        String cityOfOrigin,
        String bio,
        String occupation,
        String phoneNumber) {

    public static UpdateProfileResponse from(Profile profile) {
        return new UpdateProfileResponse(
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getUser().getUsername(),
                profile.getGender().getValue(),
                Countries.nameOf(profile.getCountryOfOrigin()),
                profile.getCityOfOrigin(),
                profile.getBio(),
                profile.getOccupation().getValue(),
                profile.getPhoneNumber());
    }
}
