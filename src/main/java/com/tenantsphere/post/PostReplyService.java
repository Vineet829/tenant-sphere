package com.tenantsphere.post;

import com.tenantsphere.post.dto.ReplyCreateRequest;
import com.tenantsphere.post.dto.ReplyResponse;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostReplyService {

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final ProfileRepository profileRepository;

    public PostReplyService(
            PostRepository postRepository,
            ReplyRepository replyRepository,
            ProfileRepository profileRepository) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ReplyResponse create(UUID postId, ReplyCreateRequest request, User author) {
        Post post = postRepository.findByExternalId(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));

        Reply reply = new Reply();
        reply.setPost(post);
        reply.setAuthor(author);
        reply.setBody(request.body());
        replyRepository.save(reply);

        return ReplyResponse.from(reply, avatarFor(author));
    }

    @Transactional(readOnly = true)
    public List<ReplyResponse> listNewestFirst(UUID postId) {
        Post post = postRepository.findByExternalId(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
        return replyRepository.findByPostPkidOrderByCreatedAtDesc(post.getPkid()).stream()
                .map(reply -> ReplyResponse.from(reply, avatarFor(reply.getAuthor())))
                .toList();
    }

    private String avatarFor(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .map(profile -> profile.getAvatar())
                .orElse(null);
    }
}
