package com.tenantsphere.issue.dto;

import com.tenantsphere.issue.Priority;
import jakarta.validation.constraints.NotBlank;

public record IssueCreateRequest(
        @NotBlank(message = "This field may not be blank.") String title,
        @NotBlank(message = "This field may not be blank.") String description,
        Priority priority) {}
