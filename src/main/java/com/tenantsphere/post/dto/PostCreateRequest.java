package com.tenantsphere.post.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PostCreateRequest(
        @NotBlank(message = "This field may not be blank.") String title,
        @NotBlank(message = "This field may not be blank.") String body,
        List<String> tags) {}
