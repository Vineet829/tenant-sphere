package com.tenantsphere.issue;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.common.BaseEntity;
import com.tenantsphere.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@Setter
public class Issue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_pkid", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_pkid", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_pkid")
    private User assignedTo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status = IssueStatus.REPORTED;

    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.LOW;

    @Column(name = "resolved_on")
    private LocalDate resolvedOn;
}
