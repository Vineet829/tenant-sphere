package com.tenantsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "This field may not be blank.") String email,
        @NotBlank(message = "This field may not be blank.") String password) {}
