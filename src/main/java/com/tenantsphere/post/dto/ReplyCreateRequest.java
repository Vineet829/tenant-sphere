package com.tenantsphere.post.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplyCreateRequest(
        @NotBlank(message = "This field may not be blank.") String body) {}
