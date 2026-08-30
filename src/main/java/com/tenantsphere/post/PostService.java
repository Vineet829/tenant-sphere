package com.tenantsphere.post;

import com.tenantsphere.common.ContentViewRepository;
import com.tenantsphere.post.dto.PostByTagResponse;
import com.tenantsphere.post.dto.PostResponse;
import com.tenantsphere.post.dto.ReplyResponse;
import com.tenantsphere.post.dto.TopPostResponse;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    public static final String CONTENT_TYPE = "post";

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DISPLAY_ZONE);

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final ContentViewRepository contentViewRepository;
    private final ProfileRepository profileRepository;

    public PostService(
            PostRepository postRepository,
            ReplyRepository replyRepository,
            ContentViewRepository contentViewRepository,
            ProfileRepository profileRepository) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.contentViewRepository = contentViewRepository;
        this.profileRepository = profileRepository;
    }

    public boolean canAuthorPosts(User user) {
        if (user.isStaff() || user.isSuperuser()) {
            return true;
        }
        return profileRepository.findByUserPkid(user.getPkid())
                .map(profile -> profile.getOccupation() == com.tenantsphere.profile.Occupation.TENANT)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> toResponses(Page<Post> posts, User viewer, boolean withRepliesCount) {
        Context context = contextFor(posts.getContent(), true, viewer);
        return posts.map(post -> toResponse(post, viewer, context, withRepliesCount));
    }

    @Transactional(readOnly = true)
    public Page<PostByTagResponse> toTagResponses(Page<Post> posts, User viewer) {
        Context context = contextFor(posts.getContent(), false, viewer);
        return posts.map(post -> {
            PostResponse full = toResponse(post, viewer, context, true);
            return new PostByTagResponse(
                    full.id(), full.title(), full.slug(), full.authorUsername(),
                    full.isBookmarked(), full.createdAt(), full.updatedAt(), full.viewCount(),
                    full.upvotes(), full.downvotes(), full.isUpvoted(), full.repliesCount(),
                    full.avatar(), full.body(), full.tags());
        });
    }

    @Transactional(readOnly = true)
    public PostResponse toResponse(Post post, User viewer) {
        return toResponse(post, viewer, contextFor(List.of(post), true, viewer), true);
    }

    @Transactional(readOnly = true)
    public List<TopPostResponse> toTopResponses(List<Post> posts) {
        Context context = contextFor(posts, false, null);
        List<TopPostResponse> out = new ArrayList<>();
        for (Post post : posts) {
            out.add(new TopPostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getSlug(),
                    post.getAuthor().getUsername(),
                    post.getUpvotes(),
                    context.viewCounts().getOrDefault(post.getPkid(), 0L),
                    context.replyCounts().getOrDefault(post.getPkid(), 0L),
                    context.avatars().get(post.getAuthor().getPkid()),
                    post.getCreatedAt()));
        }
        return out;
    }

    private PostResponse toResponse(Post post, User viewer, Context context, boolean withRepliesCount) {
        List<ReplyResponse> replies = context.replies()
                .getOrDefault(post.getPkid(), List.of())
                .stream()
                .map(reply -> ReplyResponse.from(reply, context.avatars().get(reply.getAuthor().getPkid())))
                .toList();

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getAuthor().getUsername(),
                context.bookmarked().contains(post.getPkid()),
                DISPLAY_FORMAT.format(post.getCreatedAt()),
                DISPLAY_FORMAT.format(post.getUpdatedAt()),
                context.viewCounts().getOrDefault(post.getPkid(), 0L),
                post.getUpvotes(),
                post.getDownvotes(),
                context.upvoted().contains(post.getPkid()),
                withRepliesCount ? context.replyCounts().getOrDefault(post.getPkid(), 0L) : null,
                context.avatars().get(post.getAuthor().getPkid()),
                post.getBody(),
                post.getTags().stream().map(Tag::getName).toList(),
                replies);
    }

    private Context contextFor(List<Post> posts, boolean withReplies, User viewer) {
        List<Long> pkids = posts.stream().map(Post::getPkid).toList();
        Map<Long, Long> viewCounts = new HashMap<>();
        Map<Long, List<Reply>> replies = new LinkedHashMap<>();
        Map<Long, Long> replyCounts = new HashMap<>();
        Map<Long, String> avatars = new HashMap<>();
        Set<Long> bookmarked = new HashSet<>();
        Set<Long> upvoted = new HashSet<>();

        if (pkids.isEmpty()) {
            return new Context(viewCounts, replies, replyCounts, avatars, bookmarked, upvoted);
        }

        contentViewRepository.countByObjects(CONTENT_TYPE, pkids)
                .forEach(row -> viewCounts.put(row.getObjectPkid(), row.getTotal()));

        List<Reply> allReplies = replyRepository.findByPostPkidInOrderByCreatedAtAsc(pkids);
        for (Reply reply : allReplies) {
            replies.computeIfAbsent(reply.getPost().getPkid(), key -> new ArrayList<>()).add(reply);
            replyCounts.merge(reply.getPost().getPkid(), 1L, Long::sum);
        }

        for (Profile profile : profileRepository.findAll()) {
            if (profile.getAvatar() != null) {
                avatars.put(profile.getUser().getPkid(), profile.getAvatar());
            }
        }

        if (viewer != null) {
            bookmarked.addAll(postRepository.findBookmarkedPkids(viewer.getPkid(), pkids));
            upvoted.addAll(postRepository.findUpvotedPkids(viewer.getPkid(), pkids));
        }

        if (!withReplies) {
            replies.clear();
        }
        return new Context(viewCounts, replies, replyCounts, avatars, bookmarked, upvoted);
    }

    private record Context(
            Map<Long, Long> viewCounts,
            Map<Long, List<Reply>> replies,
            Map<Long, Long> replyCounts,
            Map<Long, String> avatars,
            Set<Long> bookmarked,
            Set<Long> upvoted) {}
}
