package com.tenantsphere.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id, String username, String firstName, String lastName, String email) {}
