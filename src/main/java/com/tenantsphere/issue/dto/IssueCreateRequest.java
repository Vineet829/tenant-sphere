package com.tenantsphere.issue.dto;

import com.tenantsphere.issue.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueCreateRequest(
        @NotBlank(message = "This field may not be blank.") @Size(max = 255)
                String title,
        @NotBlank(message = "This field may not be blank.")
                @Size(max = 5000, message = "A description can be at most 5,000 characters.")
                String description,
        Priority priority) {}
