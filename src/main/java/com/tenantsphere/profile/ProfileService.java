package com.tenantsphere.profile;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.apartment.dto.ApartmentResponse;
import com.tenantsphere.profile.dto.ProfileResponse;
import com.tenantsphere.rating.RatingRepository;
import com.tenantsphere.user.User;
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

    public ProfileService(
            ProfileRepository profileRepository,
            ApartmentRepository apartmentRepository,
            RatingRepository ratingRepository) {
        this.profileRepository = profileRepository;
        this.apartmentRepository = apartmentRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse forUser(User user) {
        Profile profile = profileRepository.findByUserPkid(user.getPkid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found"));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<ProfileResponse> listTenants(Pageable pageable) {
        return profileRepository.findTenantProfiles(pageable).map(this::toResponse);
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
