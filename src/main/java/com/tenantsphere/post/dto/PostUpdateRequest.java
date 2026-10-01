package com.tenantsphere.post.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record PostUpdateRequest(
        @Size(min = 1, max = 250, message = "Title must be between 1 and 250 characters.")
        @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String title,
        @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String body,
        @Size(max = 10, message = "A post can have at most 10 tags.")
        List<@NotBlank @Size(max = 100) String> tags) {}
