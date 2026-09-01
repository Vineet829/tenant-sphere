package com.tenantsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivationRequest(@NotBlank String uid, @NotBlank String token) {}
