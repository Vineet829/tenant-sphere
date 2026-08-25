package com.tenantsphere.issue.dto;

import com.tenantsphere.issue.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record IssueCreateRequest(
        @NotNull(message = "This field is required.")
                @Pattern(regexp = ".*\\S.*", message = "This field may not be blank.")
                String title,
        @NotNull(message = "This field is required.")
                @Pattern(regexp = ".*\\S.*", message = "This field may not be blank.")
                String description,
        Priority priority) {}
