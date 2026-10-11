package com.tenantsphere.user;

import com.tenantsphere.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "auth_version", nullable = false)
    private long authVersion = 0;

    public void setPassword(String password) {
        if (this.password != null && !this.password.equals(password)) authVersion++;
        this.password = password;
    }

    public void setActive(boolean active) {
        if (this.active && !active) authVersion++;
        this.active = active;
    }

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "is_staff", nullable = false)
    private boolean staff = false;

    @Column(name = "is_superuser", nullable = false)
    private boolean superuser = false;

    @Column(name = "date_joined", nullable = false)
    private Instant dateJoined = Instant.now();

    @Column(name = "last_login")
    private Instant lastLogin;

    // When the account was first turned on, which the database sets however
    // that happens. Until then, it is waiting for its activation link.
    @Column(name = "activated_at", insertable = false, updatable = false)
    private Instant activatedAt;

    @Column(name = "is_guest", nullable = false)
    private boolean guest = false;

    @Column(name = "guest_expires_at")
    private Instant guestExpiresAt;

    public boolean isSessionEligible() {
        return active && (!guest || (guestExpiresAt != null && guestExpiresAt.isAfter(Instant.now())));
    }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }
}
