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
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "profiles")
@Getter
@Setter
public class Profile extends BaseEntity {

    public static final int DEACTIVATION_REPORTERS = 5;

    // Staff run the admin console, so reports about them are kept for the
    // other administrators to see, but never switch their account off:
    // residents together could otherwise lock every administrator out.
    public static boolean keptOnByStaffRole(boolean staff, boolean superuser) {
        return staff || superuser;
    }

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

    // Five different reporters lock a resident out: no sign-in, no activation,
    // and open sessions end. Staff stay in, as above. The owner is passed in
    // because the user association is lazy and the per-request check runs outside a
    // transaction.
    public boolean isBanned(User owner) {
        return reportCount >= DEACTIVATION_REPORTERS && !keptOnByStaffRole(owner.isStaff(), owner.isSuperuser());
    }

    public void recalculateReputation() {
        this.reputation = Math.max(0, 100 - reportCount * 20);
    }
}
