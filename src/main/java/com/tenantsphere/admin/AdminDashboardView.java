package com.tenantsphere.admin;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Route(value = "", layout = AdminLayout.class)
@PageTitle("Overview | TenantSphere Administration")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class AdminDashboardView extends VerticalLayout {
    private static final List<AdminResource> PRIMARY =
            List.of(
                    AdminResource.USERS,
                    AdminResource.APARTMENTS,
                    AdminResource.ISSUES,
                    AdminResource.REPORTS);

    public AdminDashboardView(
            AdminQueryService queries, AdminPermissions access, AdminAuditService audit) {
        addClassNames("admin-content", "admin-overview");
        Div heading =
                new Div(
                        AdminPresentation.eyebrow("Your workspace"),
                        new H1("Community overview"),
                        new Paragraph(
                                "People, homes and the everyday work that keeps them connected."));
        heading.addClassName("admin-page-heading");
        add(heading);

        Map<AdminResource, Integer> counts = new EnumMap<>(AdminResource.class);
        for (var resource : AdminResource.values()) {
            if (access.allowed(resource, "view")) {
                counts.put(resource, queries.count(resource, AdminQueryService.Query.empty()));
            }
        }
        if (counts.isEmpty()) {
            Div empty =
                    new Div(
                            AdminPresentation.icon(VaadinIcon.LOCK),
                            new H2("Your workspace is ready for access"),
                            new Paragraph(
                                    "Ask a superuser to assign a group or direct permissions to"
                                        + " your staff account."));
            empty.addClassNames("admin-panel", "admin-empty");
            add(empty);
            return;
        }

        Div metrics = new Div();
        metrics.addClassName("admin-metrics");
        for (var resource : PRIMARY) {
            if (counts.containsKey(resource)) metrics.add(metric(resource, counts.get(resource)));
        }
        if (metrics.getChildren().findAny().isPresent()) add(metrics);

        Div activity = new Div();
        activity.addClassNames("admin-panel", "admin-activity-panel");
        Div activityHeader =
                new Div(new H2("Recent changes"), new Span("The latest updates by your team."));
        activityHeader.addClassName("admin-panel-heading");
        Anchor history = new Anchor("/admin/history", "View all");
        history.add(AdminPresentation.icon(VaadinIcon.ARROW_RIGHT));
        history.addClassName("admin-text-link");
        Div top = new Div(activityHeader, history);
        top.addClassName("admin-panel-top");
        activity.add(top);
        var recent = audit.history(null, null, "", 0, 8);
        if (recent.isEmpty()) {
            Div empty =
                    new Div(
                            AdminPresentation.icon(VaadinIcon.CLOCK),
                            new H3("No changes recorded yet"),
                            new Paragraph("Updates to records you can view will appear here."));
            empty.addClassName("admin-empty");
            activity.add(empty);
        } else {
            Div timeline = new Div();
            timeline.addClassName("admin-timeline");
            recent.forEach(row -> timeline.add(activity(row)));
            activity.add(timeline);
        }

        Div resources = new Div();
        resources.addClassNames("admin-panel", "admin-resource-panel");
        Div resourcesHeader =
                new Div(
                        new H2("Community resources"),
                        new Span("More ways to manage your community."));
        resourcesHeader.addClassName("admin-panel-heading");
        resources.add(resourcesHeader);
        counts.forEach(
                (resource, count) -> {
                    if (!PRIMARY.contains(resource)) resources.add(resource(resource, count));
                });
        Div workspace = new Div(activity);
        workspace.addClassName("admin-workspace-grid");
        if (counts.keySet().stream().anyMatch(resource -> !PRIMARY.contains(resource))) {
            workspace.add(resources);
        } else workspace.addClassName("admin-workspace-single");
        add(workspace);

        Div footer =
                new Div(
                        AdminPresentation.icon(VaadinIcon.SHIELD),
                        new Span("Your permissions apply throughout this workspace."),
                        new Span("Administration changes are recorded in history."));
        footer.addClassName("admin-overview-footer");
        add(footer);
    }

    private static Anchor metric(AdminResource resource, int count) {
        Anchor card = new Anchor("/admin/" + resource.route(), "");
        card.addClassNames("admin-metric", "admin-metric-" + resource.route());
        Span symbol = new Span(AdminPresentation.icon(AdminPresentation.symbol(resource)));
        symbol.addClassName("admin-metric-icon");
        Div top = new Div(new Span(resource.title), symbol);
        top.addClassName("admin-metric-top");
        Span value = new Span(AdminPresentation.number(count));
        value.addClassName("admin-metric-value");
        Span description = new Span(AdminPresentation.description(resource));
        description.addClassName("admin-metric-description");
        Div bottom =
                new Div(
                        new Span("Manage " + resource.title.toLowerCase(java.util.Locale.ROOT)),
                        AdminPresentation.icon(VaadinIcon.ARROW_RIGHT));
        bottom.addClassName("admin-metric-bottom");
        card.add(top, value, description, bottom);
        return card;
    }

    private static Anchor resource(AdminResource resource, int count) {
        Anchor link = new Anchor("/admin/" + resource.route(), "");
        link.addClassName("admin-resource-row");
        Span label = new Span(resource.title);
        label.addClassName("admin-resource-label");
        Span total = new Span(AdminPresentation.number(count));
        total.addClassName("admin-resource-total");
        link.add(
                AdminPresentation.icon(AdminPresentation.symbol(resource)),
                label,
                total,
                AdminPresentation.icon(VaadinIcon.ARROW_RIGHT));
        return link;
    }

    private static Div activity(Map<String, Object> row) {
        String action = Objects.toString(row.get("action"), "change");
        boolean added = action.equals("add") || action.equals("create");
        boolean deleted = action.equals("delete") || action.equals("cascade_delete");
        Span symbol =
                new Span(
                        AdminPresentation.icon(
                                added
                                        ? VaadinIcon.PLUS
                                        : deleted ? VaadinIcon.TRASH : VaadinIcon.EDIT));
        symbol.addClassNames(
                "admin-activity-icon",
                added
                        ? "admin-tone-success"
                        : deleted ? "admin-tone-danger" : "admin-tone-neutral");
        String object = Objects.toString(row.get("object_label"), "Record");
        Span title = new Span((added ? "Created " : deleted ? "Deleted " : "Updated ") + object);
        title.addClassName("admin-activity-title");
        title.getElement().setAttribute("title", title.getText());
        Span actor = new Span(Objects.toString(row.get("actor_label"), "Administrator"));
        actor.addClassName("admin-muted");
        Div text = new Div(title, actor);
        text.addClassName("admin-activity-copy");
        Span time = new Span(AdminPresentation.timestamp(row.get("created_at")));
        time.addClassName("admin-activity-time");
        Div activity = new Div(symbol, text, time);
        activity.addClassName("admin-activity");
        return activity;
    }
}
