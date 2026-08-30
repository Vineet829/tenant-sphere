package com.tenantsphere.post.dto;

import java.time.Instant;
import java.util.UUID;

public record TopPostResponse(
        UUID id,
        String title,
        String slug,
        String authorUsername,
        int upvotes,
        long viewCount,
        long repliesCount,
        String avatar,
        Instant createdAt) {}
