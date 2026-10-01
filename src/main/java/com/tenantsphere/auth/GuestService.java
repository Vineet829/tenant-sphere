package com.tenantsphere.auth;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuestService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final ApartmentRepository apartments;
    private final JdbcTemplate db;
    private final boolean enabled;

    public GuestService(UserRepository users, ProfileRepository profiles, ApartmentRepository apartments,
            JdbcTemplate db, @Value("${app.guest.enabled:false}") boolean enabled) {
        this.users = users;
        this.profiles = profiles;
        this.apartments = apartments;
        this.db = db;
        this.enabled = enabled;
    }

    @Transactional
    public User create(String address) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Guest access is not available on this installation.");
        // Bound storage/creation work on the 1 GB host, including requests on different instances.
        db.queryForObject("SELECT pg_advisory_xact_lock(170628,8)", Object.class);
        long active = db.queryForObject("SELECT count(*) FROM users WHERE is_guest AND guest_expires_at > NOW()", Long.class);
        if (active >= 200) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "All demo places are in use. Please try again later.");
        String hash = addressHash(address);
        db.update("DELETE FROM guest_login_limits WHERE window_start < NOW() - INTERVAL '1 hour'");
        Integer attempts = db.queryForObject("""
                INSERT INTO guest_login_limits(address_hash,window_start,attempts) VALUES(?,NOW(),1)
                ON CONFLICT(address_hash) DO UPDATE SET attempts=guest_login_limits.attempts+1
                RETURNING attempts
                """, Integer.class, hash);
        if (attempts > 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many demo accounts created. Please try again in an hour.");

        String suffix = UUID.randomUUID().toString().replace("-", "");
        User guest = new User();
        guest.setUsername("guest_" + suffix);
        guest.setEmail(suffix + "@guest.tenantsphere.invalid");
        guest.setFirstName("Guest");
        guest.setLastName("Resident");
        guest.setPassword("!guest"); // No password can authenticate this account.
        guest.setGuest(true);
        guest.setGuestExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        guest.setActive(true);
        guest.setLastLogin(Instant.now());
        users.saveAndFlush(guest);

        Profile profile = new Profile();
        profile.setUser(guest);
        profile.setSlug(guest.getUsername());
        profile.setBio("Visiting the community.");
        profile.setPhoneNumber("");
        profile.setCityOfOrigin("");
        profile.setAvatar("/assets/avatars/cedar-court/testuser-bot-v5.jpg");
        profiles.save(profile);

        Apartment apartment = new Apartment();
        String unit = "D" + Long.toString(guest.getPkid(), 36).toUpperCase(java.util.Locale.ROOT);
        if (unit.length() > 10 || apartments.findByUnitNumber(unit).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Please try creating your demo account again.");
        }
        apartment.setUnitNumber(unit);
        apartment.setBuilding("Demo residence");
        apartment.setFloor(1);
        apartment.setTenant(guest);
        apartments.saveAndFlush(apartment);
        db.update("""
                INSERT INTO issues(id,apartment_pkid,reported_by_pkid,title,description,status,priority)
                VALUES(?,?,?,'Kitchen tap needs a new washer',
                       'The tap still drips after it is closed. It has been getting worse this week.','reported','low')
                """, UUID.randomUUID(), apartment.getPkid(), guest.getPkid());
        db.update("""
                INSERT INTO issues(id,apartment_pkid,reported_by_pkid,title,description,status,priority,resolved_on)
                VALUES(?,?,?,'Bedroom window latch',
                       'The latch has been adjusted and the window now closes properly.','resolved','medium',CURRENT_DATE)
                """, UUID.randomUUID(), apartment.getPkid(), guest.getPkid());
        db.update("""
                INSERT INTO post_bookmarks(post_pkid,user_pkid)
                SELECT pkid,? FROM posts ORDER BY created_at DESC,pkid DESC LIMIT 3
                ON CONFLICT DO NOTHING
                """, guest.getPkid());
        return guest;
    }

    private static String addressHash(String address) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(address).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
