package com.tenantsphere.post;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ContentViewRecorder;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.post.dto.PopularTagResponse;
import com.tenantsphere.post.dto.PostByTagResponse;
import com.tenantsphere.post.dto.PostResponse;
import com.tenantsphere.post.dto.PostCreateRequest;
import com.tenantsphere.post.dto.PostUpdateRequest;
import com.tenantsphere.common.MessageBody;
import com.tenantsphere.common.Slugs;
import com.tenantsphere.post.dto.TopPostResponse;
import com.tenantsphere.user.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private static final int PAGE_SIZE = 9;
    private static final int TOP_POSTS_LIMIT = 6;
    private static final int POPULAR_TAGS_LIMIT = 5;

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostService postService;
    private final ContentViewRecorder contentViewRecorder;

    public PostController(
            PostRepository postRepository,
            TagRepository tagRepository,
            PostService postService,
            ContentViewRecorder contentViewRecorder) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.postService = postService;
        this.contentViewRecorder = contentViewRecorder;
    }

    @GetMapping("/")
    @ObjectLabel("posts")
    public PageResponse<PostResponse> list(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        Page<PostResponse> result = postService.toResponses(
                postRepository.findAllOrdered(pageable(page, PAGE_SIZE)), viewer(principal), true);
        return PageResponse.from(result, url("/"));
    }

    @GetMapping("/my-posts/")
    @ObjectLabel("my_posts")
    public PageResponse<PostResponse> myPosts(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        User user = principal.getUser();
        Page<PostResponse> result = postService.toResponses(
                postRepository.findByAuthor(user.getPkid(), pageable(page, 10)), user, false);
        return PageResponse.from(result, url("/my-posts/"));
    }

    @GetMapping("/bookmarked/posts/")
    @ObjectLabel("bookmarked_posts")
    public PageResponse<PostResponse> bookmarked(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        User user = principal.getUser();
        Page<PostResponse> result = postService.toResponses(
                postRepository.findBookmarkedBy(user.getPkid(), pageable(page, 10)), user, true);
        return PageResponse.from(result, url("/bookmarked/posts/"));
    }

    @GetMapping("/top-posts/")
    @ObjectLabel("top_posts")
    public PageResponse<TopPostResponse> topPosts() {
        List<Post> posts = postRepository.findTopPosts(PageRequest.of(0, TOP_POSTS_LIMIT));
        List<TopPostResponse> responses = postService.toTopResponses(posts);
        Page<TopPostResponse> page =
                new PageImpl<>(responses, PageRequest.of(0, Math.max(responses.size(), 1)), responses.size());
        return PageResponse.from(page, url("/top-posts/"));
    }

    @GetMapping("/popular-tags/")
    @ObjectLabel("popular_tags")
    public PageResponse<PopularTagResponse> popularTags() {
        List<PopularTagResponse> tags = tagRepository
                .findPopular(PageRequest.of(0, POPULAR_TAGS_LIMIT))
                .stream()
                .map(row -> new PopularTagResponse(row.getName(), row.getSlug(), row.getPostCount()))
                .toList();
        Page<PopularTagResponse> page =
                new PageImpl<>(tags, PageRequest.of(0, Math.max(tags.size(), 1)), tags.size());
        return PageResponse.from(page, url("/popular-tags/"));
    }

    @GetMapping("/tags/{tagSlug}/")
    @ObjectLabel("posts_by_tag")
    public PageResponse<PostByTagResponse> byTag(
            @PathVariable String tagSlug,
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        Page<PostByTagResponse> result = postService.toTagResponses(
                postRepository.findByTagSlug(tagSlug, pageable(page, 10)), viewer(principal));
        return PageResponse.from(result, url("/tags/" + tagSlug + "/"));
    }

    @GetMapping("/{slug}/")
    @ObjectLabel("post")
    public PostResponse detail(
            @PathVariable String slug,
            @AuthenticationPrincipal AppUserDetails principal,
            HttpServletRequest request) {

        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
        contentViewRecorder.record(
                PostService.CONTENT_TYPE, post.getPkid(), viewer(principal), request);
        return postService.toResponse(post, viewer(principal));
    }

    @PostMapping("/create/")
    @ObjectLabel("post")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        User user = principal.getUser();
        if (!postService.canAuthorPosts(user)) {
            throw new AccessDeniedException(
                    "You do not have permission to create of edit this post.");
        }

        Post post = new Post();
        post.setTitle(request.title());
        post.setSlug(uniqueSlug(Slugs.of(request.title())));
        post.setBody(request.body());
        post.setAuthor(user);
        if (request.tags() != null) {
            post.setTags(resolveTags(request.tags()));
        }
        postRepository.save(post);
        return postService.toResponse(post, user);
    }

    @DeleteMapping("/{postId}/delete/")
    public ResponseEntity<MessageBody> delete(
            @PathVariable UUID postId, @AuthenticationPrincipal AppUserDetails principal) {

        Post post = postRepository.findByExternalId(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));

        if (!post.getAuthor().getPkid().equals(principal.getUser().getPkid())) {
            throw new AccessDeniedException("You do not have permission to delete this post.");
        }
        postRepository.delete(post);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new MessageBody("Post deleted successfully!"));
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (postRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private Set<Tag> resolveTags(List<String> names) {
        Set<Tag> tags = new LinkedHashSet<>();
        for (String name : names) {
            tags.add(tagRepository.findByName(name).orElseGet(() -> {
                Tag tag = new Tag();
                tag.setName(name);
                tag.setSlug(Slugs.of(name));
                return tagRepository.save(tag);
            }));
        }
        return tags;
    }

    @PatchMapping("/{slug}/update/")
    @ObjectLabel("post")
    public PostResponse update(
            @PathVariable String slug,
            @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));

        User user = principal.getUser();
        if (!post.getAuthor().getPkid().equals(user.getPkid())) {
            throw new AccessDeniedException("You do not have permission to edit this post.");
        }

        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.body() != null) {
            post.setBody(request.body());
        }
        if (request.tags() != null) {
            post.setTags(resolveTags(request.tags()));
        }
        postRepository.save(post);
        return postService.toResponse(post, user);
    }

    private User viewer(AppUserDetails principal) {
        return principal == null ? null : principal.getUser();
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }

    private String url(String suffix) {
        return "http://localhost:8080/api/v1/posts" + suffix;
    }
}
