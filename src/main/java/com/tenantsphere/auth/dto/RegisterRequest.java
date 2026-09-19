package com.tenantsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "^[a-zA-Z0-9_]+$") String username,
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8) @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit") String password,
        @NotBlank String rePassword) {}
