package com.tenantsphere.auth;

import com.tenantsphere.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "google_identities")
@Getter
@Setter
public class GoogleIdentity {
    @Id
    @Column(length = 255)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_pkid", nullable = false, unique = true)
    private User user;
}
