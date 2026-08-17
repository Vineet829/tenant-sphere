package com.tenantsphere.auth.dto;

import com.tenantsphere.profile.Gender;
import com.tenantsphere.profile.Occupation;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.user.User;
import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String username,
        String fullName,
        String slug,
        Gender gender,
        Occupation occupation,
        String phoneNumber,
        String country,
        String city,
        int reputation,
        Instant dateJoined) {

    public static CurrentUserResponse from(User user, Profile profile) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getFullName(),
                profile.getSlug(),
                profile.getGender(),
                profile.getOccupation(),
                profile.getPhoneNumber(),
                profile.getCountryOfOrigin(),
                profile.getCityOfOrigin(),
                profile.getReputation(),
                user.getDateJoined());
    }
}
