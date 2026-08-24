package com.tenantsphere.issue;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.issue.dto.IssueCreateRequest;
import com.tenantsphere.issue.dto.IssueStatusUpdateRequest;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.user.User;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IssueWriteService {

    private final IssueRepository issueRepository;
    private final ApartmentRepository apartmentRepository;
    private final MailService mailService;

    public IssueWriteService(
            IssueRepository issueRepository,
            ApartmentRepository apartmentRepository,
            MailService mailService) {
        this.issueRepository = issueRepository;
        this.apartmentRepository = apartmentRepository;
        this.mailService = mailService;
    }

    @Transactional
    public Issue create(UUID apartmentId, IssueCreateRequest request, User reporter) {
        Apartment apartment = apartmentRepository.findByExternalId(apartmentId)
                .filter(a -> a.getTenant() != null
                        && a.getTenant().getPkid().equals(reporter.getPkid()))
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have permission to report an issue for this apartment. "
                                + "its not yours"));

        Issue issue = new Issue();
        issue.setApartment(apartment);
        issue.setReportedBy(reporter);
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setStatus(IssueStatus.REPORTED);
        if (request.priority() != null) {
            issue.setPriority(request.priority());
        }
        issueRepository.save(issue);

        mailService.send(
                reporter.getEmail(),
                "Issue Report Confirmation",
                "Your issue \"" + issue.getTitle() + "\" has been logged on "
                        + mailService.siteName() + ".");
        return issue;
    }

    @Transactional
    public Issue updateStatus(UUID issueId, IssueStatusUpdateRequest request, User actor) {
        Issue issue = requireIssue(issueId);

        boolean assignee = issue.getAssignedTo() != null
                && issue.getAssignedTo().getPkid().equals(actor.getPkid());
        if (!actor.isStaff() && !assignee) {
            throw new AccessDeniedException("You do not have permission to update the issue");
        }

        IssueStatus previous = issue.getStatus();
        if (request.title() != null) {
            issue.setTitle(request.title());
        }
        if (request.description() != null) {
            issue.setDescription(request.description());
        }
        if (request.status() != null) {
            issue.setStatus(request.status());
        }

        boolean nowResolved = issue.getStatus() == IssueStatus.RESOLVED
                && previous != IssueStatus.RESOLVED;
        if (nowResolved) {
            issue.setResolvedOn(LocalDate.now());
        }
        issueRepository.save(issue);

        if (nowResolved) {
            mailService.send(
                    issue.getReportedBy().getEmail(),
                    "Issue Resolved",
                    "Your issue \"" + issue.getTitle() + "\" has been resolved on "
                            + mailService.siteName() + ".");
        }
        return issue;
    }

    @Transactional
    public void delete(UUID issueId, User actor) {
        Issue issue = requireIssue(issueId);
        boolean reporter = issue.getReportedBy().getPkid().equals(actor.getPkid());
        if (!reporter && !actor.isStaff()) {
            throw new AccessDeniedException("You do not have permission to delete this issue");
        }
        issueRepository.delete(issue);
    }

    @Transactional(readOnly = true)
    public Issue requireVisible(UUID issueId, User actor) {
        Issue issue = requireIssue(issueId);
        boolean reporter = issue.getReportedBy().getPkid().equals(actor.getPkid());
        boolean assignee = issue.getAssignedTo() != null
                && issue.getAssignedTo().getPkid().equals(actor.getPkid());
        if (!reporter && !assignee && !actor.isStaff()) {
            throw new AccessDeniedException("You do not have permission to view this issue");
        }
        return issue;
    }

    private Issue requireIssue(UUID issueId) {
        return issueRepository.findByExternalId(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));
    }
}
