package com.tenantsphere.admin;

import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.report.Report;
import com.tenantsphere.report.ReportRepository;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@Route(value = "admin/reports", layout = AdminLayout.class)
@PageTitle("Reports | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ReportAdminView extends AdminGridView<Report> {

    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;

    public ReportAdminView(ReportRepository reportRepository, ProfileRepository profileRepository) {
        super("Reports", Report.class);
        this.reportRepository = reportRepository;
        this.profileRepository = profileRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(Report::getTitle).setHeader("Title").setAutoWidth(true).setSortable(true);
        grid.addColumn(report -> report.getReportedBy().getUsername())
                .setHeader("Reported by").setAutoWidth(true);
        grid.addColumn(report -> report.getReportedUser().getUsername())
                .setHeader("Reported user").setAutoWidth(true);
        grid.addColumn(this::reportCount).setHeader("Report Count").setAutoWidth(true);
        grid.addColumn(Report::getCreatedAt).setHeader("Created at").setAutoWidth(true).setSortable(true);

        onSearch(this::refresh);
        refresh("");
    }

    private int reportCount(Report report) {
        return profileRepository.findByUserPkid(report.getReportedUser().getPkid())
                .map(profile -> profile.getReportCount())
                .orElse(0);
    }

    private void refresh(String term) {
        List<Report> reports = reportRepository.findAllBy().stream()
                .filter(report -> term.isBlank()
                        || report.getTitle().toLowerCase().contains(term.toLowerCase())
                        || report.getReportedUser().getFirstName().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        grid.setItems(reports);
    }
}
