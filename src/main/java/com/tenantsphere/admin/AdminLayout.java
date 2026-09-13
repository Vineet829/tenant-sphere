package com.tenantsphere.admin;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public AdminLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        addToNavbar(new DrawerToggle(), header());
        addToDrawer(navigation());
    }

    private HorizontalLayout header() {
        H1 title = new H1("Tenant Sphere Admin");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Span user = new Span(currentUsername());
        user.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Margin.Right.MEDIUM);

        Button logout = new Button("Log out", event -> authenticationContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        logout.addClassNames(LumoUtility.Margin.Right.MEDIUM);

        HorizontalLayout header = new HorizontalLayout(title, user, logout);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.expand(title);
        return header;
    }

    private SideNav navigation() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", AdminDashboardView.class));
        nav.addItem(new SideNavItem("Users", UserAdminView.class));
        nav.addItem(new SideNavItem("Profiles", ProfileAdminView.class));
        nav.addItem(new SideNavItem("Apartments", ApartmentAdminView.class));
        nav.addItem(new SideNavItem("Issues", IssueAdminView.class));
        nav.addItem(new SideNavItem("Posts", PostAdminView.class));
        nav.addItem(new SideNavItem("Ratings", RatingAdminView.class));
        nav.addItem(new SideNavItem("Reports", ReportAdminView.class));
        nav.addItem(new SideNavItem("Content views", ContentViewAdminView.class));
        return nav;
    }

    private String currentUsername() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("");
    }
}
