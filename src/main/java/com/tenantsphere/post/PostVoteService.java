package com.tenantsphere.post;

import com.tenantsphere.user.User;
import com.tenantsphere.post.dto.PostVoteResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostVoteService {

    private final PostRepository postRepository;
    private final PostVoteRepository votes;

    public PostVoteService(PostRepository postRepository, PostVoteRepository votes) {
        this.postRepository = postRepository;
        this.votes = votes;
    }

    @Transactional
    public PostVoteResponse upvote(UUID postId, User user) {
        return changeVote(postId, user, 1, true);
    }

    @Transactional
    public PostVoteResponse downvote(UUID postId, User user) {
        return changeVote(postId, user, -1, true);
    }

    @Transactional
    public PostVoteResponse setVote(UUID postId, User user, int vote) {
        if (vote < -1 || vote > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote must be -1, 0 or 1.");
        }
        return changeVote(postId, user, vote, false);
    }

    private PostVoteResponse changeVote(UUID postId, User user, int requested, boolean toggle) {
        long post = votes.lockPost(postId);
        int previous = votes.currentVote(post, user.getPkid());
        int next = toggle && previous == requested ? 0 : requested;
        String message = next == 1 ? "Post upvoted successfully!"
                : next == -1 ? "Post downvoted successfully!"
                : previous == 1 ? "Upvote removed"
                : previous == -1 ? "Downvote removed" : "Vote removed";
        return votes.setVote(post, user.getPkid(), next, message);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(String slug, User user) {
        Post post = requireBySlug(slug);
        return !postRepository.findBookmarkedPkids(user.getPkid(), java.util.List.of(post.getPkid()))
                .isEmpty();
    }

    @Transactional
    public void bookmark(String slug, User user) {
        Post post = requireBySlug(slug);
        post.getBookmarkedBy().add(user);
        postRepository.save(post);
    }

    @Transactional
    public void unbookmark(String slug, User user) {
        Post post = requireBySlug(slug);
        post.getBookmarkedBy().removeIf(u -> u.getPkid().equals(user.getPkid()));
        postRepository.save(post);
    }

    private Post requireBySlug(String slug) {
        return postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
    }
}
