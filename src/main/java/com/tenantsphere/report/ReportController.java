package com.tenantsphere.report;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.report.dto.ReportCreateRequest;
import com.tenantsphere.report.dto.ReportResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final int PAGE_SIZE = 10;
    private static final String LIST_URL = "http://localhost:8080/api/v1/reports/me/";

    private final ReportRepository reportRepository;
    private final ReportWriteService reportWriteService;

    public ReportController(
            ReportRepository reportRepository, ReportWriteService reportWriteService) {
        this.reportRepository = reportRepository;
        this.reportWriteService = reportWriteService;
    }

    @PostMapping("/create/")
    @ObjectLabel("report")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return ReportResponse.from(reportWriteService.create(request, principal.getUser()));
    }

    @GetMapping("/me/")
    @ObjectLabel("reports")
    public PageResponse<ReportResponse> myReports(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        PageRequest pageable = PageRequest.of(
                Math.max(page - 1, 0),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReportResponse> result = reportRepository
                .findByReportedByPkid(principal.getUser().getPkid(), pageable)
                .map(ReportResponse::from);

        return PageResponse.from(result, LIST_URL);
    }
}
