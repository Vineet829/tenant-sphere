package com.tenantsphere.report;

import com.tenantsphere.common.Slugs;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.report.dto.ReportCreateRequest;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ReportWriteService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MailService mailService;

    public ReportWriteService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            MailService mailService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mailService = mailService;
    }

    @Transactional
    public Report create(ReportCreateRequest request, User reporter) {
        User reported = userRepository.findByUsername(request.reportedUserUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "The provided username does not exist"));

        Report report = new Report();
        report.setTitle(request.title());
        report.setSlug(uniqueSlug(Slugs.of(request.title())));
        report.setDescription(request.description());
        report.setReportedBy(reporter);
        report.setReportedUser(reported);
        reportRepository.save(report);

        Profile profile = profileRepository.findByUserPkid(reported.getPkid()).orElseThrow();
        profile.setReportCount((int) reportRepository.countByReportedUserPkid(reported.getPkid()));
        profile.recalculateReputation();
        profileRepository.save(profile);

        if (profile.getReportCount() == 1) {
            mailService.send(
                    reported.getEmail(),
                    "Warning: " + reported.getFullName() + " You have been reported!",
                    "A report titled \"" + report.getTitle() + "\" has been filed against you on "
                            + mailService.siteName() + ".");
        } else if (profile.getReportCount() >= 5) {
            reported.setActive(false);
            userRepository.save(reported);
            mailService.send(
                    reported.getEmail(),
                    "Account Deactivation and Eviction Notice! : " + reported.getFullName(),
                    "Your account on " + mailService.siteName()
                            + " has been deactivated following repeated reports.");
        }
        return report;
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (reportRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
