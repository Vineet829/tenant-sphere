package com.tenantsphere.profile;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.apartment.dto.ApartmentResponse;
import com.tenantsphere.common.Countries;
import com.tenantsphere.profile.dto.ProfileResponse;
import com.tenantsphere.profile.dto.UpdateProfileRequest;
import com.tenantsphere.profile.dto.UpdateProfileResponse;
import com.tenantsphere.rating.RatingRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ApartmentRepository apartmentRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            ApartmentRepository apartmentRepository,
            RatingRepository ratingRepository,
            UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.apartmentRepository = apartmentRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse forUser(User user) {
        Profile profile = profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found"));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<ProfileResponse> listTenants(Pageable pageable, String search) {
        if (hasValue(search)) {
            return profileRepository
                    .searchTenantProfiles(search.trim().toLowerCase(Locale.ROOT), pageable)
                    .map(this::toResponse);
        }
        return profileRepository.findTenantProfiles(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProfileResponse> listNonTenants(Pageable pageable, String search) {
        if (hasValue(search)) {
            return profileRepository
                    .searchNonTenantProfiles(search.trim().toLowerCase(Locale.ROOT), pageable)
                    .map(this::toResponse);
        }
        return profileRepository.findNonTenantProfiles(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UpdateProfileResponse forUpdate(User user) {
        return UpdateProfileResponse.from(requireProfile(user));
    }

    @Transactional
    public UpdateProfileResponse update(User user, UpdateProfileRequest request) {
        Profile profile = requireProfile(user);
        User owner = profile.getUser();

        if (hasValue(request.firstName())) {
            owner.setFirstName(request.firstName().trim());
        }
        if (hasValue(request.lastName())) {
            owner.setLastName(request.lastName().trim());
        }
        if (hasValue(request.username())) {
            applyUsername(owner, request.username().trim());
        }
        if (hasValue(request.gender())) {
            profile.setGender(parseGender(request.gender()));
        }
        if (hasValue(request.occupation())) {
            profile.setOccupation(parseOccupation(request.occupation()));
        }
        if (hasValue(request.countryOfOrigin())) {
            profile.setCountryOfOrigin(parseCountry(request.countryOfOrigin()));
        }
        if (hasValue(request.cityOfOrigin())) {
            profile.setCityOfOrigin(request.cityOfOrigin().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (hasValue(request.phoneNumber())) {
            profile.setPhoneNumber(request.phoneNumber().trim());
        }

        userRepository.save(owner);
        profileRepository.save(profile);
        return UpdateProfileResponse.from(profile);
    }

    private Profile requireProfile(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found"));
    }

    private void applyUsername(User owner, String username) {
        if (owner.getUsername().equalsIgnoreCase(username)) {
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A user with this username already exists");
        }
        owner.setUsername(username);
    }

    private Gender parseGender(String value) {
        try {
            return Gender.fromValue(value.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "\"" + value + "\" is not a valid choice.");
        }
    }

    private Occupation parseOccupation(String value) {
        try {
            return Occupation.fromValue(value.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "\"" + value + "\" is not a valid choice.");
        }
    }

    private String parseCountry(String value) {
        return Countries.codeOf(value)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "\"" + value + "\" is not a valid country."));
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private ProfileResponse toResponse(Profile profile) {
        ApartmentResponse apartment = apartmentRepository
                .findFirstByTenantPkidOrderByPkidAsc(profile.getUser().getPkid())
                .map(ApartmentResponse::from)
                .orElse(null);
        double average = ratingRepository
                .averageForUser(profile.getUser().getPkid())
                .orElse(0.0);
        return ProfileResponse.of(profile, apartment, average);
    }
}
