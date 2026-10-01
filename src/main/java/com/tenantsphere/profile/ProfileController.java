package com.tenantsphere.profile;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.MessageBody;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.profile.dto.ProfileResponse;
import com.tenantsphere.profile.dto.UpdateProfileRequest;
import com.tenantsphere.profile.dto.UpdateProfileResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final AvatarService avatarService;

    public ProfileController(ProfileService profileService, AvatarService avatarService) {
        this.profileService = profileService;
        this.avatarService = avatarService;
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
            @RequestParam(name = "page_size", defaultValue = "9") int pageSize,
            @RequestParam(name = "search", required = false) String search) {

        int size = Math.min(Math.max(pageSize, 1), 100);
        Page<ProfileResponse> result =
                profileService.listTenants(PageRequest.of(Math.max(page - 1, 0), size), search);
        return PageResponse.from(result, PageResponse.currentRequestUrl());
    }

    @GetMapping("/non-tenant-profiles/")
    @ObjectLabel("non_tenant_profiles")
    public PageResponse<ProfileResponse> listNonTenants(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "9") int pageSize,
            @RequestParam(name = "search", required = false) String search) {

        int size = Math.min(Math.max(pageSize, 1), 100);
        Page<ProfileResponse> result =
                profileService.listNonTenants(PageRequest.of(Math.max(page - 1, 0), size), search);
        return PageResponse.from(result, PageResponse.currentRequestUrl());
    }

    @GetMapping("/user/update/")
    @ObjectLabel("profile")
    public UpdateProfileResponse getForUpdate(@AuthenticationPrincipal AppUserDetails principal) {
        return profileService.forUpdate(principal.getUser());
    }

    @PatchMapping("/user/update/")
    @ObjectLabel("profile")
    public UpdateProfileResponse update(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return profileService.update(principal.getUser(), request);
    }

    @PatchMapping("/user/avatar/")
    public ResponseEntity<MessageBody> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            @AuthenticationPrincipal AppUserDetails principal) {
        avatarService.upload(principal.getUser(), avatar);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageBody("Avatar updated successfully."));
    }
}
