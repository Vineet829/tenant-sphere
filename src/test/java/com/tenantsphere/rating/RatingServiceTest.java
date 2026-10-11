package com.tenantsphere.rating;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenantsphere.profile.Occupation;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.rating.dto.RatingCreateRequest;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {
    @Mock private RatingRepository ratings;
    @Mock private UserRepository users;
    @Mock private ProfileRepository profiles;

    private RatingService service;

    @BeforeEach
    void setUp() {
        service = new RatingService(ratings, users, profiles);
    }

    @Test
    void aTenantRatesATechnician() {
        User tenant = user(1L, Occupation.TENANT);
        user(2L, Occupation.PLUMBER, "plumber");

        service.create(new RatingCreateRequest("plumber", 4, "Fixed the leak"), tenant);

        verify(ratings).save(any(Rating.class));
    }

    @Test
    void aTechnicianCantRateATenant() {
        User technician = user(1L, Occupation.PLUMBER);
        user(2L, Occupation.TENANT, "tenant");

        assertThatThrownBy(() -> service.create(new RatingCreateRequest("tenant", 1, null), technician))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only tenants can review technicians.");
        verify(ratings, never()).save(any());
    }

    @Test
    void aGuestCantRateAnyone() {
        User guest = new User();
        guest.setPkid(1L);
        guest.setGuest(true);

        assertThatThrownBy(() -> service.create(new RatingCreateRequest("plumber", 1, null), guest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageStartingWith("Guests can't rate technicians");
        verify(ratings, never()).save(any());
    }

    private User user(long pkid, Occupation occupation) {
        User user = new User();
        user.setPkid(pkid);
        Profile profile = new Profile();
        profile.setOccupation(occupation);
        when(profiles.findByUserPkid(pkid)).thenReturn(Optional.of(profile));
        return user;
    }

    private User user(long pkid, Occupation occupation, String username) {
        User user = user(pkid, occupation);
        user.setUsername(username);
        when(users.findByUsername(username)).thenReturn(Optional.of(user));
        return user;
    }
}
