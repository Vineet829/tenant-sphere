package com.tenantsphere.profile.dto;

import com.tenantsphere.apartment.dto.ApartmentResponse;
import com.tenantsphere.common.Countries;
import com.tenantsphere.profile.Gender;
import com.tenantsphere.profile.Occupation;
import com.tenantsphere.profile.Profile;
import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String slug,
        String firstName,
        String lastName,
        String username,
        String fullName,
        Gender gender,
        String countryOfOrigin,
        String cityOfOrigin,
        String bio,
        Occupation occupation,
        int reputation,
        Instant dateJoined,
        String avatar,
        ApartmentResponse apartment,
        double averageRating) {

    public static ProfileResponse of(
            Profile profile, ApartmentResponse apartment, double averageRating) {
        return new ProfileResponse(
                profile.getId(),
                profile.getSlug(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getUser().getUsername(),
                profile.getUser().getFullName(),
                profile.getGender(),
                Countries.nameOf(profile.getCountryOfOrigin()),
                profile.getCityOfOrigin(),
                profile.getBio(),
                profile.getOccupation(),
                profile.getReputation(),
                profile.getUser().getDateJoined(),
                profile.getAvatar(),
                apartment,
                averageRating);
    }
}
