package com.tenantsphere.profile;

import com.tenantsphere.common.PageResponse;
import com.tenantsphere.post.Post;
import com.tenantsphere.post.PostRepository;
import com.tenantsphere.post.ReplyRepository;
import com.tenantsphere.profile.dto.ProfilePostResponse;
import com.tenantsphere.profile.dto.ProfileReviewResponse;
import com.tenantsphere.profile.dto.ProfileReviewsResponse;
import com.tenantsphere.profile.dto.ProfileReviewsResponse.RatingBucket;
import com.tenantsphere.rating.Rating;
import com.tenantsphere.rating.RatingRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProfileActivityService {

    private final ProfileRepository profiles;
    private final PostRepository posts;
    private final ReplyRepository replies;
    private final RatingRepository ratings;

    public ProfileActivityService(
            ProfileRepository profiles, PostRepository posts,
            ReplyRepository replies, RatingRepository ratings) {
        this.profiles = profiles;
        this.posts = posts;
        this.replies = replies;
        this.ratings = ratings;
    }

    public Page<ProfilePostResponse> posts(String slug, Pageable pageable) {
        Profile profile = directoryProfile(slug);
        Page<Post> page = posts.findByAuthorPkidOrderByCreatedAtDescPkidDesc(
                profile.getUser().getPkid(), pageable);
        Map<Long, Long> replyCounts = new HashMap<>();
        if (page.hasContent()) {
            replies.countByPosts(page.map(Post::getPkid).getContent())
                    .forEach(count -> replyCounts.put(count.getPostPkid(), count.getTotal()));
        }
        return page.map(post -> ProfilePostResponse.from(
                post, replyCounts.getOrDefault(post.getPkid(), 0L)));
    }

    public ProfileReviewsResponse reviews(String slug, Pageable pageable, String pageUrl) {
        Profile profile = directoryProfile(slug);
        Long userPkid = profile.getUser().getPkid();
        Page<Rating> page = ratings.findByRatedUserPkidOrderByCreatedAtDescPkidDesc(userPkid, pageable);

        Map<Long, String> avatars = new HashMap<>();
        if (page.hasContent()) {
            profiles.findAllByUserPkidIn(page.stream()
                            .map(rating -> rating.getRatingUser().getPkid()).distinct().toList())
                    .forEach(author -> avatars.put(author.getUser().getPkid(), author.getAvatar()));
        }
        Page<ProfileReviewResponse> reviews = page.map(rating -> ProfileReviewResponse.from(
                rating, avatars.get(rating.getRatingUser().getPkid())));

        Map<Integer, Long> counts = new HashMap<>();
        ratings.countByStars(userPkid).forEach(count -> counts.put(count.getStars(), count.getTotal()));
        var distribution = new ArrayList<RatingBucket>();
        long total = 0;
        long weightedTotal = 0;
        for (int stars = 5; stars >= 1; stars--) {
            long count = counts.getOrDefault(stars, 0L);
            distribution.add(new RatingBucket(stars, count));
            total += count;
            weightedTotal += stars * count;
        }
        double average = total == 0 ? 0.0 : (double) weightedTotal / total;
        return new ProfileReviewsResponse(average, distribution, PageResponse.from(reviews, pageUrl));
    }

    private Profile directoryProfile(String slug) {
        return profiles.findBySlug(slug)
                .filter(profile -> !profile.getUser().isStaff() && !profile.getUser().isSuperuser())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }
}
