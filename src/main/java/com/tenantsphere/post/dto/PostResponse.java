package com.tenantsphere.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String slug,
        String authorUsername,
        boolean isBookmarked,
        String createdAt,
        String updatedAt,
        long viewCount,
        int upvotes,
        int downvotes,
        boolean isUpvoted,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long repliesCount,
        String avatar,
        String body,
        List<String> tags,
        List<ReplyResponse> replies) {}
