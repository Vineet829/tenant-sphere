package com.tenantsphere.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotBlank(message = "This field may not be blank.") @Size(max = 255) String title,
        @NotBlank(message = "This field may not be blank.")
        @Size(max = 5000, message = "A description can be at most 5,000 characters.") String description,
        @NotBlank(message = "This field may not be blank.") String reportedUserUsername) {}
