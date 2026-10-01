package com.tenantsphere.auth;

import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GuestServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final ApartmentRepository apartments = mock(ApartmentRepository.class);
    private final JdbcTemplate db = mock(JdbcTemplate.class);

    @Test void createsIndependentUnprivilegedAccountsWithNoReusablePassword() {
        when(db.queryForObject(contains("count(*) FROM users"), eq(Long.class))).thenReturn(0L);
        when(db.queryForObject(contains("INSERT INTO guest_login_limits"), eq(Integer.class), anyString())).thenReturn(1);
        var ids = new AtomicLong();
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            User user = call.getArgument(0); user.setPkid(ids.incrementAndGet()); user.setId(UUID.randomUUID()); return user;
        });
        var service = new GuestService(users, profiles, apartments, db, true);
        User first = service.create("192.0.2.1"), second = service.create("192.0.2.1");
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getUsername()).isNotEqualTo(second.getUsername());
        assertThat(first.getEmail()).isNotEqualTo(second.getEmail()).endsWith("@guest.tenantsphere.invalid");
        assertThat(first.isGuest()).isTrue(); assertThat(first.isStaff() || first.isSuperuser()).isFalse();
        assertThat(first.getPassword()).isEqualTo("!guest");
        assertThat(Duration.between(Instant.now(),first.getGuestExpiresAt()).toMinutes()).isBetween(1439L,1440L);
        verify(profiles, times(2)).save(argThat(profile -> profile.getAvatar().endsWith("testuser-bot-v5.jpg")));
        verify(apartments, times(2)).saveAndFlush(any());
    }
    @Test void disabledGuestAccessDoesNotTouchTheDatabase() {
        assertThatThrownBy(() -> new GuestService(users, profiles, apartments, db, false).create("192.0.2.1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        verifyNoInteractions(db, users, profiles, apartments);
    }
    @Test void guestCapacityIsBoundedBeforeCreatingAnyAccounts() {
        when(db.queryForObject(contains("count(*) FROM users"),eq(Long.class))).thenReturn(200L);
        assertThatThrownBy(() -> new GuestService(users,profiles,apartments,db,true).create("192.0.2.1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
        verifyNoInteractions(users,profiles,apartments);
    }
    @Test void perAddressLimitIsCheckedBeforeAccountCreation() {
        when(db.queryForObject(contains("count(*) FROM users"), eq(Long.class))).thenReturn(0L);
        when(db.queryForObject(contains("INSERT INTO guest_login_limits"),eq(Integer.class),anyString())).thenReturn(6);
        assertThatThrownBy(() -> new GuestService(users,profiles,apartments,db,true).create("192.0.2.1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
        verifyNoInteractions(users,profiles,apartments);
    }
}
