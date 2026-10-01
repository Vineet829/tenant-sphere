package com.tenantsphere.post;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.MessageBody;
import com.tenantsphere.user.User;
import com.tenantsphere.post.dto.PostVoteRequest;
import com.tenantsphere.post.dto.PostVoteResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostVoteController {

    private final PostVoteService voteService;

    public PostVoteController(PostVoteService voteService) {
        this.voteService = voteService;
    }

    @PatchMapping("/{postId}/upvote/")
    public PostVoteResponse upvote(
            @PathVariable UUID postId, @AuthenticationPrincipal AppUserDetails principal) {
        return voteService.upvote(postId, principal.getUser());
    }

    @PatchMapping("/{postId}/downvote/")
    public PostVoteResponse downvote(
            @PathVariable UUID postId, @AuthenticationPrincipal AppUserDetails principal) {
        return voteService.downvote(postId, principal.getUser());
    }

    @PutMapping("/{postId}/vote/")
    public PostVoteResponse setVote(
            @PathVariable UUID postId, @Valid @RequestBody PostVoteRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return voteService.setVote(postId, principal.getUser(), request.vote());
    }

    @PatchMapping("/{slug}/bookmark/")
    public ResponseEntity<MessageBody> bookmark(
            @PathVariable String slug, @AuthenticationPrincipal AppUserDetails principal) {
        User user = principal.getUser();
        if (voteService.isBookmarked(slug, user)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageBody("Post already bookmarked"));
        }
        voteService.bookmark(slug, user);
        return ResponseEntity.ok(new MessageBody("Post bookmarked"));
    }

    @PatchMapping("/{slug}/unbookmark/")
    public ResponseEntity<MessageBody> unbookmark(
            @PathVariable String slug, @AuthenticationPrincipal AppUserDetails principal) {
        User user = principal.getUser();
        if (!voteService.isBookmarked(slug, user)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageBody(
                            "You can't remove a bookmark that did not exist in the first place "));
        }
        voteService.unbookmark(slug, user);
        return ResponseEntity.ok(new MessageBody("Post Bookmark Removed"));
    }
}
