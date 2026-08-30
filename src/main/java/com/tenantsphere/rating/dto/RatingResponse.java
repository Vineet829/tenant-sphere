package com.tenantsphere.rating.dto;

import com.tenantsphere.rating.Rating;
import java.util.UUID;

public record RatingResponse(UUID id, Integer rating, String comment) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(rating.getId(), rating.getRating(), rating.getComment());
    }
}
