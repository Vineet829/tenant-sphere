package com.tenantsphere.admin;

import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.common.ContentViewRepository;
import com.tenantsphere.issue.IssueRepository;
import com.tenantsphere.post.PostRepository;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.rating.RatingRepository;
import com.tenantsphere.report.ReportRepository;
import com.tenantsphere.user.UserRepository;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin", layout = AdminLayout.class)
@PageTitle("Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(
            UserRepository users,
            ProfileRepository profiles,
            ApartmentRepository apartments,
            IssueRepository issues,
            PostRepository posts,
            RatingRepository ratings,
            ReportRepository reports,
            ContentViewRepository contentViews) {

        add(new H2("Site administration"));

        HorizontalLayout tiles = new HorizontalLayout();
        tiles.setWidthFull();
        tiles.getStyle().set("flex-wrap", "wrap");
        tiles.add(
                tile("Users", users.count(), UserAdminView.class),
                tile("Profiles", profiles.count(), ProfileAdminView.class),
                tile("Apartments", apartments.count(), ApartmentAdminView.class),
                tile("Issues", issues.count(), IssueAdminView.class),
                tile("Posts", posts.count(), PostAdminView.class),
                tile("Ratings", ratings.count(), RatingAdminView.class),
                tile("Reports", reports.count(), ReportAdminView.class),
                tile("Content views", contentViews.count(), ContentViewAdminView.class));
        add(tiles);
    }

    private VerticalLayout tile(String label, long count, Class<? extends com.vaadin.flow.component.Component> target) {
        Span value = new Span(String.valueOf(count));
        value.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        RouterLink link = new RouterLink(label, target);

        VerticalLayout tile = new VerticalLayout(value, link);
        tile.setSpacing(false);
        tile.setPadding(true);
        tile.setWidth("190px");
        tile.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)");
        return tile;
    }
}
