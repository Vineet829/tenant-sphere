package com.tenantsphere.user;

import com.tenantsphere.common.BaseEntity;
import com.tenantsphere.profile.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
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

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Profile profile;

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }
}
