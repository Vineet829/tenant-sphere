package com.tenantsphere.report.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportCreateRequest(
        @NotBlank(message = "This field may not be blank.") String title,
        @NotBlank(message = "This field may not be blank.") String description,
        @NotBlank(message = "This field may not be blank.") String reportedUserUsername) {}
