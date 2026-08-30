package com.tenantsphere.post.dto;

import com.tenantsphere.post.Reply;
import java.time.Instant;
import java.util.UUID;

public record ReplyResponse(
        UUID id,
        Long post,
        String authorUsername,
        String body,
        String avatar,
        Instant createdAt,
        Instant updatedAt) {

    public static ReplyResponse from(Reply reply, String avatar) {
        return new ReplyResponse(
                reply.getId(),
                reply.getPost().getPkid(),
                reply.getAuthor().getUsername(),
                reply.getBody(),
                avatar,
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
