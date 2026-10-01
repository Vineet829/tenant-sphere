package com.tenantsphere.profile.dto;

import com.tenantsphere.common.PageResponse;
import java.util.List;

public record ProfileReviewsResponse(
        double averageRating, List<RatingBucket> distribution,
        PageResponse<ProfileReviewResponse> reviews) {

    public record RatingBucket(int stars, long count) {}
}
