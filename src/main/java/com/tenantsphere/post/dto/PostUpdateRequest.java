package com.tenantsphere.post.dto;

import java.util.List;

public record PostUpdateRequest(String title, String body, List<String> tags) {}
