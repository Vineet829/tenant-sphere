package com.tenantsphere.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.*;

@Route(value = "history", layout = AdminLayout.class)
@PageTitle("Change history | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class AdminHistoryView extends VerticalLayout {
    public AdminHistoryView(AdminAuditService audit) {
        setSizeFull();
        addClassNames("admin-content", "admin-history");
        Div heading =
                new Div(
                        AdminPresentation.eyebrow("Administration"),
                        new H1("Change history"),
                        new Paragraph(
                                "A record of your team’s updates, including deleted records."));
        heading.addClassName("admin-page-heading");
        add(heading);
        var history = table(audit, null, null);
        add(history);
        expand(history);
    }

    static void openHistory(AdminAuditService audit, AdminResource resource, long id) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("History · " + resource.singular + " #" + id);
        dialog.setWidth("1100px");
        dialog.setMaxWidth("96vw");
        dialog.setHeight("75vh");
        dialog.add(table(audit, resource, id));
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private static VerticalLayout table(AdminAuditService audit, AdminResource resource, Long id) {
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addClassName("admin-record-grid");
        grid.getElement().setAttribute("aria-label", "Administration change history");
        grid.setSizeFull();
        for (String key :
                List.of("created_at", "actor_label", "resource", "object_label", "action"))
            grid.addColumn(
                            row ->
                                    key.equals("created_at")
                                            ? AdminPresentation.timestamp(row.get(key))
                                            : AdminCrudView.display(row.get(key)))
                    .setHeader(AdminCrudView.label(key))
                    .setAutoWidth(true);
        grid.addComponentColumn(
                        row -> {
                            Button changes = new Button("View changes", event -> details(row));
                            changes.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                            return changes;
                        })
                .setHeader("Changes")
                .setAutoWidth(true);
        TextField search = new TextField();
        search.setPlaceholder("Search by account or record");
        search.setAriaLabel("Search history");
        search.setWidth("min(400px,100%)");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(AdminPresentation.icon(VaadinIcon.SEARCH));
        search.setValueChangeMode(ValueChangeMode.LAZY);
        int[] page = {0};
        Span position = new Span();
        Button previous = new Button("Previous"), next = new Button("Next");
        Runnable load =
                () -> {
                    var rows = audit.history(resource, id, search.getValue(), page[0] * 50, 51);
                    grid.setItems(rows.stream().limit(50).toList());
                    previous.setEnabled(page[0] > 0);
                    next.setEnabled(rows.size() > 50);
                    position.setText(
                            rows.isEmpty() ? "No changes recorded" : "Page " + (page[0] + 1));
                };
        previous.addClickListener(
                event -> {
                    page[0]--;
                    load.run();
                });
        next.addClickListener(
                event -> {
                    page[0]++;
                    load.run();
                });
        search.addValueChangeListener(
                event -> {
                    page[0] = 0;
                    load.run();
                });
        HorizontalLayout pagination = new HorizontalLayout(previous, position, next);
        pagination.addClassName("admin-pagination");
        pagination.setAlignItems(Alignment.CENTER);
        VerticalLayout wrapper = new VerticalLayout(search, grid, pagination);
        wrapper.addClassName("admin-history-table");
        wrapper.setPadding(false);
        wrapper.setSizeFull();
        wrapper.expand(grid);
        load.run();
        return wrapper;
    }

    private record Change(String field, String before, String after) {}

    private static void details(Map<String, Object> record) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Recorded changes");
        dialog.setWidth("1000px");
        dialog.setMaxWidth("94vw");
        dialog.setHeight("65vh");
        Map<String, Object> before = parse(record.get("before_data")),
                after = parse(record.get("after_data"));
        Set<String> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<Change> changes =
                keys.stream()
                        .filter(k -> !Objects.equals(before.get(k), after.get(k)))
                        .map(
                                k ->
                                        new Change(
                                                AdminCrudView.label(k),
                                                AdminCrudView.display(before.get(k)),
                                                AdminCrudView.display(after.get(k))))
                        .toList();
        Grid<Change> grid = new Grid<>(Change.class, false);
        grid.addColumn(Change::field).setHeader("Field").setWidth("180px").setFlexGrow(0);
        grid.addColumn(Change::before).setHeader("Before");
        grid.addColumn(Change::after).setHeader("After");
        grid.addThemeName("wrap-cell-content");
        grid.setItems(changes);
        grid.setSizeFull();
        dialog.add(grid);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private static Map<String, Object> parse(Object data) {
        if (data == null) return Map.of();
        try {
            return new ObjectMapper()
                    .readValue(data.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("record", data.toString());
        }
    }
}
