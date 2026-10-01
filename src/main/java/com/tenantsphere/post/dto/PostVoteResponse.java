package com.tenantsphere.post.dto;

import java.util.UUID;

public record PostVoteResponse(
        UUID id, String slug, int upvotes, int downvotes,
        boolean isUpvoted, boolean isDownvoted, String message) {}
