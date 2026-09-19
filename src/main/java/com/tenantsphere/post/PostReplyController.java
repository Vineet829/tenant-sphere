package com.tenantsphere.post;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.post.dto.ReplyCreateRequest;
import com.tenantsphere.post.dto.ReplyResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostReplyController {

    private static final int PAGE_SIZE = 10;

    private final PostReplyService replyService;

    public PostReplyController(PostReplyService replyService) {
        this.replyService = replyService;
    }

    @PostMapping("/{postId}/reply/")
    @ObjectLabel("reply")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse create(
            @PathVariable UUID postId,
            @Valid @RequestBody ReplyCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return replyService.create(postId, request, principal.getUser());
    }

    @GetMapping("/{postId}/replies/")
    @ObjectLabel("replies")
    public PageResponse<ReplyResponse> list(
            @PathVariable UUID postId,
            @RequestParam(name = "page", defaultValue = "1") int page) {
        List<ReplyResponse> all = replyService.listNewestFirst(postId);
        int pageIndex = Math.max(page - 1, 0);
        int start = Math.min(pageIndex * PAGE_SIZE, all.size());
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<ReplyResponse> pageContent = all.subList(start, end);
        return PageResponse.from(
                new PageImpl<>(pageContent, PageRequest.of(pageIndex, PAGE_SIZE), all.size()),
                "http://localhost:8080/api/v1/posts/" + postId + "/replies/");
    }
}
