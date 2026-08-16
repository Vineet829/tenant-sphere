package com.tenantsphere.common;

import com.tenantsphere.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "content_views")
@Getter
@Setter
public class ContentView extends BaseEntity {

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "object_pkid", nullable = false)
    private Long objectPkid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pkid")
    private User user;

    @Column(name = "viewer_ip", length = 45)
    private String viewerIp;

    @Column(name = "last_viewed", nullable = false)
    private Instant lastViewed = Instant.now();
}
