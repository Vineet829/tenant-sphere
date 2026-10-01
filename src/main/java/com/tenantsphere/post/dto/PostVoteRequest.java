package com.tenantsphere.post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PostVoteRequest(@NotNull @Min(-1) @Max(1) Integer vote) {}
