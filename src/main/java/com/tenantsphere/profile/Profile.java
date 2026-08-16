package com.tenantsphere.profile;

import com.tenantsphere.common.BaseEntity;
import com.tenantsphere.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profiles")
@Getter
@Setter
public class Profile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_pkid", nullable = false, unique = true)
    private User user;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender = Gender.OTHER;

    @Column(name = "bio")
    private String bio;

    @Column(name = "occupation", nullable = false, length = 20)
    private Occupation occupation = Occupation.TENANT;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber = "+919997008000";

    @Column(name = "country_of_origin", nullable = false, length = 2)
    private String countryOfOrigin = "IN";

    @Column(name = "city_of_origin", nullable = false, length = 180)
    private String cityOfOrigin = "New Delhi";

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "reputation", nullable = false)
    private int reputation = 100;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    public boolean isBanned() {
        return reportCount >= 5;
    }

    public void recalculateReputation() {
        this.reputation = Math.max(0, 100 - reportCount * 20);
    }
}
