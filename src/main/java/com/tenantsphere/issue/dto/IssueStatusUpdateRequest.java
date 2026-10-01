package com.tenantsphere.issue.dto;

import com.tenantsphere.issue.IssueStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IssueStatusUpdateRequest(
        @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String title,
        @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String description,
        IssueStatus status) {}
