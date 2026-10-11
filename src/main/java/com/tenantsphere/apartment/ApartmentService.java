package com.tenantsphere.apartment;

import com.tenantsphere.apartment.dto.CreateApartmentRequest;
import com.tenantsphere.profile.Occupation;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final ProfileRepository profileRepository;

    public ApartmentService(
            ApartmentRepository apartmentRepository, ProfileRepository profileRepository) {
        this.apartmentRepository = apartmentRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Apartment create(User user, CreateApartmentRequest request) {
        if (!user.isSuperuser() && !isTenant(user)) {
            throw new AccessDeniedException(
                    "You are not allowed to create an apartment, you are not a tenant");
        }

        // A tenant lives in one apartment; claiming more would keep other residents out of theirs.
        if (!user.isSuperuser() && hasApartment(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You already have an apartment. Ask an administrator to change it.");
        }

        if (apartmentRepository.findByUnitNumber(request.unitNumber()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "apartment with this unit number already exists.");
        }

        Apartment apartment = new Apartment();
        apartment.setUnitNumber(request.unitNumber());
        apartment.setBuilding(request.building());
        apartment.setFloor(request.floor());
        apartment.setTenant(user);
        return apartmentRepository.save(apartment);
    }

    // The profile is locked first, so two requests at once can't both find no apartment.
    private boolean hasApartment(User user) {
        profileRepository.findByUserPkid(user.getPkid())
                .ifPresent(profile -> profileRepository.findByPkidForUpdate(profile.getPkid()));
        return apartmentRepository.findFirstByTenantPkidOrderByPkidAsc(user.getPkid()).isPresent();
    }

    private boolean isTenant(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .map(Profile::getOccupation)
                .filter(occupation -> occupation == Occupation.TENANT)
                .isPresent();
    }
}
