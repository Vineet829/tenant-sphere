package com.tenantsphere.issue;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.issue.dto.IssueCreateRequest;
import com.tenantsphere.issue.dto.IssueResponse;
import com.tenantsphere.issue.dto.IssueStatusUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.tenantsphere.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

    private static final int PAGE_SIZE = 10;
    private static final String STAFF_ONLY =
            "Access to this information is restricted to staff and admin users only";

    private final IssueRepository issueRepository;
    private final IssueService issueService;
    private final IssueWriteService issueWriteService;

    public IssueController(
            IssueRepository issueRepository,
            IssueService issueService,
            IssueWriteService issueWriteService) {
        this.issueRepository = issueRepository;
        this.issueService = issueService;
        this.issueWriteService = issueWriteService;
    }

    @GetMapping("/")
    @ObjectLabel("issues")
    public PageResponse<IssueResponse> allIssues(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        User user = principal.getUser();
        if (!user.isStaff() && !user.isSuperuser()) {
            throw new AccessDeniedException(STAFF_ONLY);
        }
        Page<IssueResponse> result = issueService.toResponses(issueRepository.findAllBy(pageable(page)));
        return PageResponse.from(result, url("/"));
    }

    @GetMapping("/me/")
    @ObjectLabel("my_issues")
    public PageResponse<IssueResponse> myIssues(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        Page<IssueResponse> result = issueService.toResponses(
                issueRepository.findByReportedByPkid(principal.getUser().getPkid(), pageable(page)));
        return PageResponse.from(result, url("/me/"));
    }

    @GetMapping("/assigned/")
    @ObjectLabel("assigned_issues")
    public PageResponse<IssueResponse> assignedIssues(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        Page<IssueResponse> result = issueService.toResponses(
                issueRepository.findByAssignedToPkid(principal.getUser().getPkid(), pageable(page)));
        return PageResponse.from(result, url("/assigned/"));
    }

    @PostMapping("/create/{apartmentId}/")
    @ObjectLabel("issue")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(
            @PathVariable UUID apartmentId,
            @Valid @RequestBody IssueCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        Issue issue = issueWriteService.create(apartmentId, request, principal.getUser());
        return IssueResponse.of(issue, 0L);
    }

    @PatchMapping("/update/{issueId}/")
    @ObjectLabel("issue")
    public IssueResponse update(
            @PathVariable UUID issueId,
            @RequestBody IssueStatusUpdateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        Issue issue = issueWriteService.updateStatus(issueId, request, principal.getUser());
        return IssueResponse.of(issue, 0L);
    }

    @GetMapping("/{issueId}/")
    @ObjectLabel("issue")
    public IssueResponse detail(
            @PathVariable UUID issueId, @AuthenticationPrincipal AppUserDetails principal) {
        Issue issue = issueWriteService.requireVisible(issueId, principal.getUser());
        return IssueResponse.of(issue, 0L);
    }

    @DeleteMapping("/delete/{issueId}/")
    public ResponseEntity<Void> delete(
            @PathVariable UUID issueId, @AuthenticationPrincipal AppUserDetails principal) {
        issueWriteService.delete(issueId, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    private PageRequest pageable(int page) {
        return PageRequest.of(
                Math.max(page - 1, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    private String url(String suffix) {
        return "http://localhost:8080/api/v1/issues" + suffix;
    }
}
