package com.tenantsphere.issue.dto;

import com.tenantsphere.issue.IssueStatus;

public record IssueStatusUpdateRequest(
        String title, String description, IssueStatus status) {}
