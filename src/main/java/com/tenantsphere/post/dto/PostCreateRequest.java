package com.tenantsphere.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostCreateRequest(
        @NotBlank(message = "This field may not be blank.") @Size(max = 250) String title,
        @NotBlank(message = "This field may not be blank.")
        @Size(max = 10000, message = "A post can be at most 10,000 characters.") String body,
        @Size(max = 10, message = "A post can have at most 10 tags.")
        List<@NotBlank @Size(max = 100) String> tags) {}
