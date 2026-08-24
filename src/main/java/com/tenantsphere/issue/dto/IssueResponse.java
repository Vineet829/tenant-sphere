package com.tenantsphere.issue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tenantsphere.issue.Issue;
import com.tenantsphere.issue.IssueStatus;
import com.tenantsphere.issue.Priority;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        String apartmentUnit,
        String reportedBy,
        @JsonInclude(JsonInclude.Include.NON_NULL) String assignedTo,
        String title,
        String description,
        IssueStatus status,
        Priority priority,
        long viewCount) {

    public static IssueResponse of(Issue issue, long viewCount) {
        return new IssueResponse(
                issue.getId(),
                issue.getApartment().getUnitNumber(),
                issue.getReportedBy().getFullName(),
                issue.getAssignedTo() == null ? null : issue.getAssignedTo().getFullName(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getPriority(),
                viewCount);
    }
}
