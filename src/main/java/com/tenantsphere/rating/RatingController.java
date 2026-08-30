package com.tenantsphere.rating;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.rating.dto.RatingCreateRequest;
import com.tenantsphere.rating.dto.RatingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/create/")
    @ObjectLabel("rating")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse create(
            @Valid @RequestBody RatingCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return RatingResponse.from(ratingService.create(request, principal.getUser()));
    }
}
