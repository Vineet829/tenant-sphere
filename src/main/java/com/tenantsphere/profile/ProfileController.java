package com.tenantsphere.profile;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.profile.dto.ProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private static final int PAGE_SIZE = 9;
    private static final String LIST_URL = "http://localhost:8080/api/v1/profiles/all/";

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/user/my-profile/")
    @ObjectLabel("profile")
    public ProfileResponse myProfile(@AuthenticationPrincipal AppUserDetails principal) {
        return profileService.forUser(principal.getUser());
    }

    @GetMapping("/all/")
    @ObjectLabel("profiles")
    public PageResponse<ProfileResponse> listTenants(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "9") int pageSize) {

        int size = Math.min(Math.max(pageSize, 1), 100);
        Page<ProfileResponse> result =
                profileService.listTenants(PageRequest.of(Math.max(page - 1, 0), size));
        return PageResponse.from(result, LIST_URL);
    }
}
