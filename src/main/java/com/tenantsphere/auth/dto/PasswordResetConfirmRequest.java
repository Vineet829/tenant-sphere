package com.tenantsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String uid,
        @NotBlank String token,
        @NotBlank @Size(min = 8) @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit") String newPassword,
        @NotBlank String reNewPassword) {}
