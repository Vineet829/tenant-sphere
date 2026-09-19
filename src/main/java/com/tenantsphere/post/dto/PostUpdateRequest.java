package com.tenantsphere.post.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record PostUpdateRequest(
        @Size(min = 1, max = 250, message = "Title must be between 1 and 250 characters.") String title,
        @Size(min = 1, message = "This field may not be blank.") String body,
        List<String> tags) {}
