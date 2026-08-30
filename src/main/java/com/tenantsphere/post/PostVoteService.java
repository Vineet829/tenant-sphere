package com.tenantsphere.post;

import com.tenantsphere.user.User;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostVoteService {

    private final PostRepository postRepository;

    public PostVoteService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public void upvote(UUID postId, User user) {
        Post post = requireById(postId);
        boolean changed = false;

        if (post.getDownvotedBy().removeIf(u -> u.getPkid().equals(user.getPkid()))) {
            post.setDownvotes(Math.max(0, post.getDownvotes() - 1));
            changed = true;
        }
        if (post.getUpvotedBy().stream().noneMatch(u -> u.getPkid().equals(user.getPkid()))) {
            post.getUpvotedBy().add(user);
            post.setUpvotes(post.getUpvotes() + 1);
            changed = true;
        }
        if (changed) {
            postRepository.save(post);
        }
    }

    @Transactional
    public void downvote(UUID postId, User user) {
        Post post = requireById(postId);

        if (post.getUpvotedBy().removeIf(u -> u.getPkid().equals(user.getPkid()))) {
            post.setUpvotes(Math.max(0, post.getUpvotes() - 1));
        }
        if (post.getDownvotedBy().stream().anyMatch(u -> u.getPkid().equals(user.getPkid()))) {
            post.getDownvotedBy().removeIf(u -> u.getPkid().equals(user.getPkid()));
            post.setDownvotes(Math.max(0, post.getDownvotes() - 1));
        } else {
            post.getDownvotedBy().add(user);
            post.setDownvotes(post.getDownvotes() + 1);
        }
        postRepository.save(post);
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

    private Post requireById(UUID postId) {
        return postRepository.findByExternalId(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
    }

    private Post requireBySlug(String slug) {
        return postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
    }
}
