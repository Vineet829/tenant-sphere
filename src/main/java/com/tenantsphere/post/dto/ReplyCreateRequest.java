package com.tenantsphere.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyCreateRequest(
        @NotBlank(message = "This field may not be blank.")
        @Size(max = 5000, message = "A reply can be at most 5,000 characters.") String body) {}
