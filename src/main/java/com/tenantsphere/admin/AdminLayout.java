package com.tenantsphere.admin;

import com.tenantsphere.config.AppProperties;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;

@RolesAllowed({"STAFF", "SUPERUSER"})
public class AdminLayout extends AppLayout {
    public AdminLayout(AdminPermissions access, AppProperties properties) {
        var actor = access.actor();
        addClassName("admin-shell");
        setPrimarySection(Section.DRAWER);

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Toggle navigation");
        Span title = new Span("Community workspace");
        title.addClassName("admin-header-title");
        Anchor community = new Anchor(properties.site().domain(), "Open community");
        community.add(AdminPresentation.icon(VaadinIcon.EXTERNAL_LINK));
        community.addClassName("admin-community-link");
        Button logout =
                new Button(
                        "Log out",
                        AdminPresentation.icon(VaadinIcon.SIGN_OUT),
                        event ->
                                getUI().ifPresent(
                                                ui ->
                                                        ui.getPage()
                                                                .executeJs(
                                                                        "fetch('/api/v1/auth/csrf/', {credentials:'same-origin'}).then(r => {"
                                                                            + " if (!r.ok) throw new Error('Unable to log out');"
                                                                            + " const cookies=document.cookie.split('; '); const token=cookies.find(c=>c.startsWith('__Host-XSRF-TOKEN=')) || cookies.find(c=>c.startsWith('XSRF-TOKEN='));"
                                                                            + " if (!token) throw new Error('Missing security token');"
                                                                            + " const f=document.createElement('form');f.method='post';f.action='/admin/logout';"
                                                                            + " const input=document.createElement('input');input.type='hidden';input.name='_csrf';input.value=decodeURIComponent(token.substring(token.indexOf('=')+1));f.appendChild(input);"
                                                                            + " document.body.appendChild(f);f.submit();"
                                                                            + " }).catch(() => window.alert('Unable to log out. Please reload and try again.'));")));
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.addClassName("admin-logout");
        Div header = new Div(toggle, title, community, logout);
        header.addClassName("admin-header");
        addToNavbar(header);

        SideNav overview = new SideNav();
        overview.getElement().setAttribute("aria-label", "Overview");
        overview.addItem(
                new SideNavItem(
                        "Overview",
                        AdminDashboardView.class,
                        AdminPresentation.icon(VaadinIcon.GRID_SMALL)));
        Div navigation = new Div(overview);
        navigation.addClassName("admin-navigation");
        addGroup(
                navigation,
                access,
                "Community",
                AdminResource.USERS,
                AdminResource.PROFILES,
                AdminResource.APARTMENTS);
        addGroup(
                navigation,
                access,
                "Operations",
                AdminResource.ISSUES,
                AdminResource.REPORTS,
                AdminResource.RATINGS);
        addGroup(
                navigation,
                access,
                "Content",
                AdminResource.POSTS,
                AdminResource.TAGS,
                AdminResource.CONTENT_VIEWS);
        SideNav administration = new SideNav();
        administration.getElement().setAttribute("aria-label", "Administration");
        if (access.allowed(AdminResource.GROUPS, "view")) {
            administration.addItem(
                    new SideNavItem(
                            "Groups",
                            AdminResource.GROUPS.route(),
                            AdminPresentation.icon(VaadinIcon.SHIELD)));
        }
        administration.addItem(
                new SideNavItem(
                        "Change history",
                        AdminHistoryView.class,
                        AdminPresentation.icon(VaadinIcon.CLOCK)));
        navigation.add(group("Administration", administration));

        String initials =
                actor.email()
                        .substring(0, Math.min(2, actor.email().length()))
                        .toUpperCase(Locale.ROOT);
        Span avatar = new Span(initials);
        avatar.addClassName("admin-account-avatar");
        avatar.getElement().setAttribute("aria-hidden", "true");
        Span role = new Span(actor.superuser() ? "Superuser" : "Staff account");
        role.addClassName("admin-account-role");
        Span email = new Span(actor.email());
        email.addClassName("admin-account-email");
        email.getElement().setAttribute("title", actor.email());
        Div identity = new Div(role, email);
        identity.addClassName("admin-account-identity");
        Div account = new Div(avatar, identity);
        account.addClassName("admin-account");

        Anchor mobileCommunity = new Anchor(properties.site().domain(), "Open community");
        mobileCommunity.add(AdminPresentation.icon(VaadinIcon.EXTERNAL_LINK));
        mobileCommunity.addClassNames("admin-community-link", "admin-drawer-community");
        Div drawer = new Div(AdminPresentation.brand(), navigation, mobileCommunity, account);
        drawer.addClassName("admin-drawer");
        addToDrawer(drawer);
    }

    private static void addGroup(
            Div navigation, AdminPermissions access, String label, AdminResource... resources) {
        SideNav nav = new SideNav();
        nav.getElement().setAttribute("aria-label", label);
        for (var resource : resources) {
            if (access.allowed(resource, "view")) {
                // SideNav resolves paths relative to the /admin servlet mapping.
                nav.addItem(
                        new SideNavItem(
                                resource.title,
                                resource.route(),
                                AdminPresentation.icon(AdminPresentation.symbol(resource))));
            }
        }
        if (!nav.getItems().isEmpty()) navigation.add(group(label, nav));
    }

    private static Div group(String label, SideNav nav) {
        Div group = new Div(AdminPresentation.eyebrow(label), nav);
        group.addClassName("admin-nav-group");
        return group;
    }
}
