package com.tenantsphere.apartment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenantsphere.apartment.dto.CreateApartmentRequest;
import com.tenantsphere.profile.Occupation;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {
    private static final CreateApartmentRequest REQUEST = new CreateApartmentRequest("B12", "North", 3);

    @Mock private ApartmentRepository apartments;
    @Mock private ProfileRepository profiles;

    private ApartmentService service;
    private User tenant;
    private Profile profile;

    @BeforeEach
    void setUp() {
        service = new ApartmentService(apartments, profiles);
        tenant = new User();
        tenant.setPkid(7L);
        profile = new Profile();
        profile.setPkid(70L);
        profile.setOccupation(Occupation.TENANT);
        when(profiles.findByUserPkid(7L)).thenReturn(Optional.of(profile));
    }

    @Test
    void aTenantAddsTheirApartment() {
        when(apartments.findFirstByTenantPkidOrderByPkidAsc(7L)).thenReturn(Optional.empty());
        when(apartments.findByUnitNumber("B12")).thenReturn(Optional.empty());

        service.create(tenant, REQUEST);

        verify(profiles).findByPkidForUpdate(70L);
        verify(apartments).save(any(Apartment.class));
    }

    @Test
    void aTenantWithAnApartmentCantClaimAnother() {
        when(apartments.findFirstByTenantPkidOrderByPkidAsc(7L)).thenReturn(Optional.of(new Apartment()));

        assertThatThrownBy(() -> service.create(tenant, REQUEST))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You already have an apartment");
        verify(apartments, never()).save(any());
    }
}
