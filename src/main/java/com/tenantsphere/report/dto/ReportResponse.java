package com.tenantsphere.report.dto;

import com.tenantsphere.report.Report;
import java.time.Instant;
import java.util.UUID;

public record ReportResponse(UUID id, String title, String description, Instant createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(), report.getTitle(), report.getDescription(), report.getCreatedAt());
    }
}
