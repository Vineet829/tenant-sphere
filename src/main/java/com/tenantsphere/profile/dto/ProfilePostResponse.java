package com.tenantsphere.profile.dto;

import com.tenantsphere.post.Post;
import java.time.Instant;
import java.util.UUID;

public record ProfilePostResponse(
        UUID id, String title, String slug, String body, Instant createdAt,
        int upvotes, int downvotes, long repliesCount) {

    public static ProfilePostResponse from(Post post, long repliesCount) {
        return new ProfilePostResponse(
                post.getId(), post.getTitle(), post.getSlug(), post.getBody(),
                post.getCreatedAt(), post.getUpvotes(), post.getDownvotes(), repliesCount);
    }
}
