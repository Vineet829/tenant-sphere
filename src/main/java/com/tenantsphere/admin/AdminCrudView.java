package com.tenantsphere.admin;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.*;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.*;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.data.value.ValueChangeMode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Shared, server-authorized editor and paged list for the registered Django models. */
public abstract class AdminCrudView extends VerticalLayout {
    protected final AdminResource resource;
    private final AdminQueryService queries;
    private final AdminRecordService writes;
    private final AdminDeleteService deletes;
    private final AdminPermissions access;
    private final AdminAuditService audit;
    private final Grid<Map<String, Object>> grid = new Grid<>();
    private final TextField search = new TextField();
    private final DatePicker from = new DatePicker("Created from"),
            to = new DatePicker("Created through");
    private final Map<String, Object> filters = new LinkedHashMap<>();
    private final Span total = new Span(), selected = new Span();
    private final List<Component> filterComponents = new ArrayList<>();
    private final Button deleteSelected = new Button("Delete selected"),
            applyStatus = new Button("Apply status");
    private final Select<String> bulkStatus = new Select<>();

    protected AdminCrudView(
            AdminResource resource,
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions access,
            AdminAuditService audit) {
        this.resource = resource;
        this.queries = queries;
        this.writes = writes;
        this.deletes = deletes;
        this.access = access;
        this.audit = audit;
        setSizeFull();
        addClassNames("admin-content", "admin-records");
        H1 heading = new H1(resource.title);
        Paragraph description = new Paragraph(AdminPresentation.description(resource));
        description.addClassName("admin-muted");
        Button add = new Button("Add " + resource.singular, event -> run(() -> openEditor(null)));
        add.setIcon(AdminPresentation.icon(VaadinIcon.PLUS));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header =
                new HorizontalLayout(
                        new VerticalLayout(
                                AdminPresentation.eyebrow("Community management"),
                                heading,
                                description),
                        add);
        header.addClassName("admin-page-header");
        header.getComponentAt(0).getElement().getClassList().add("admin-page-heading");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(header.getComponentAt(0));
        ((VerticalLayout) header.getComponentAt(0)).setPadding(false);
        ((VerticalLayout) header.getComponentAt(0)).setSpacing(false);
        add(header);
        if (!access.allowed(resource, "view")) {
            add.setVisible(false);
            add(
                    new Paragraph(
                            "You do not have permission to view this section. Contact a superuser"
                                    + " for access."));
            return;
        }
        add.setVisible(access.allowed(resource, "add"));

        search.setPlaceholder("Search " + resource.title.toLowerCase());
        search.setAriaLabel("Search " + resource.title.toLowerCase());
        search.setClearButtonVisible(true);
        search.setPrefixComponent(AdminPresentation.icon(VaadinIcon.SEARCH));
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("min(360px, 100%)");
        Button refresh =
                new Button(
                        "Refresh", AdminPresentation.icon(VaadinIcon.REFRESH), event -> refresh());
        refresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        total.addClassName("admin-record-count");
        HorizontalLayout toolbar = new HorizontalLayout(search, total, refresh);
        toolbar.addClassName("admin-toolbar");
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.expand(search);
        add(toolbar);
        search.addValueChangeListener(event -> refresh());
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.addClassName("admin-filters");
        filterBar.setWidthFull();
        filterBar.setAlignItems(Alignment.END);
        for (var f : resource.fields) {
            if (f.kind() == AdminResource.Kind.CHOICE || f.kind() == AdminResource.Kind.BOOLEAN) {
                Select<String> choice = new Select<>();
                choice.setLabel(f.label());
                choice.setItems(
                        f.kind() == AdminResource.Kind.BOOLEAN
                                ? List.of("true", "false")
                                : f.choices());
                choice.setEmptySelectionAllowed(true);
                choice.setEmptySelectionCaption("All");
                choice.setWidth("150px");
                choice.addValueChangeListener(
                        event -> {
                            filters.put(f.key(), event.getValue());
                            refresh();
                        });
                filterBar.add(choice);
                filterComponents.add(choice);
            } else if (Set.of("building", "floor", "rating").contains(f.key())) {
                TextField field = new TextField(f.label());
                field.setClearButtonVisible(true);
                field.setWidth("140px");
                field.setValueChangeMode(ValueChangeMode.LAZY);
                field.addValueChangeListener(
                        event -> {
                            filters.put(f.key(), event.getValue());
                            refresh();
                        });
                filterBar.add(field);
                filterComponents.add(field);
            }
        }
        from.setWidth("165px");
        to.setWidth("165px");
        from.setClearButtonVisible(true);
        to.setClearButtonVisible(true);
        from.addValueChangeListener(e -> refresh());
        to.addValueChangeListener(e -> refresh());
        filterBar.add(
                from,
                to,
                new Button(
                        "Clear filters",
                        event -> {
                            filters.clear();
                            filterComponents.forEach(
                                    c -> {
                                        if (c instanceof HasValue<?, ?> v) v.clear();
                                    });
                            from.clear();
                            to.clear();
                            search.clear();
                            refresh();
                        }));
        add(filterBar);

        grid.setSizeFull();
        grid.setPageSize(50);
        grid.addClassName("admin-record-grid");
        grid.getElement().setAttribute("aria-label", resource.title);
        grid.setColumnReorderingAllowed(true);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        ((GridMultiSelectionModel<Map<String, Object>>) grid.getSelectionModel())
                .setSelectAllCheckboxVisibility(
                        GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN);
        grid.addSelectionListener(
                event -> {
                    selected.setText(event.getAllSelectedItems().size() + " selected");
                    deleteSelected.setEnabled(!event.getAllSelectedItems().isEmpty());
                    applyStatus.setEnabled(!event.getAllSelectedItems().isEmpty());
                });
        String label =
                resource == AdminResource.PROFILES
                        ? "user_label"
                        : resource == AdminResource.CONTENT_VIEWS
                                ? "content_label"
                                : resource.labelColumn;
        grid.addComponentColumn(
                        row -> {
                            Button record =
                                    new Button(
                                            display(row.get(label)),
                                            event -> run(() -> openEditor(id(row))));
                            record.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                            record.addClassName("admin-record-link");
                            return record;
                        })
                .setHeader(
                        resource.singular.substring(0, 1).toUpperCase()
                                + resource.singular.substring(1))
                .setKey(label)
                .setSortProperty(label)
                .setAutoWidth(true)
                .setFlexGrow(1);
        for (String column : AdminQueryService.columns(resource)) {
            if (column.equals(label)) continue;
            grid.addColumn(row -> display(row.get(column)))
                    .setHeader(label(column))
                    .setKey(column)
                    .setSortProperty(column)
                    .setAutoWidth(true)
                    .setFlexGrow(0);
        }
        grid.addComponentColumn(
                        row -> {
                            Button history =
                                    new Button("History", event -> run(() -> openHistory(id(row))));
                            history.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                            return history;
                        })
                .setHeader("History")
                .setFrozenToEnd(true)
                .setAutoWidth(true)
                .setFlexGrow(0);
        add(grid);
        expand(grid);
        deleteSelected.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteSelected.setVisible(access.allowed(resource, "delete"));
        deleteSelected.setEnabled(false);
        deleteSelected.addClickListener(event -> run(() -> confirmDelete(selection(), null)));
        HorizontalLayout bulk = new HorizontalLayout(selected, deleteSelected);
        bulk.addClassName("admin-bulk-actions");
        bulk.setAlignItems(Alignment.CENTER);
        selected.setText("0 selected");
        if (resource == AdminResource.ISSUES && access.allowed(resource, "change")) {
            bulkStatus.setItems("reported", "in_progress", "resolved");
            bulkStatus.setValue("in_progress");
            bulkStatus.setAriaLabel("Bulk issue status");
            applyStatus.setEnabled(false);
            applyStatus.addClickListener(event -> run(() -> confirmStatus()));
            bulk.add(bulkStatus, applyStatus);
        }
        add(bulk);
        refresh();
    }

    private AdminQueryService.Query query(com.vaadin.flow.data.provider.Query<?, ?> q) {
        var sort = q.getSortOrders().isEmpty() ? null : q.getSortOrders().getFirst();
        return new AdminQueryService.Query(
                search.getValue(),
                new LinkedHashMap<>(filters),
                from.getValue(),
                to.getValue(),
                sort == null ? "pkid" : sort.getSorted(),
                sort == null || sort.getDirection() == SortDirection.DESCENDING);
    }

    private void refresh() {
        run(
                () -> {
                    grid.deselectAll();
                    var provider =
                            new CallbackDataProvider<Map<String, Object>, Void>(
                                    q ->
                                            queries
                                                    .list(
                                                            resource,
                                                            query(q),
                                                            q.getOffset(),
                                                            q.getLimit())
                                                    .stream(),
                                    q -> queries.count(resource, query(q)),
                                    row -> row.get("pkid"));
                    grid.setDataProvider(provider);
                    total.setText(
                            queries.count(
                                            resource,
                                            new AdminQueryService.Query(
                                                    search.getValue(),
                                                    new LinkedHashMap<>(filters),
                                                    from.getValue(),
                                                    to.getValue(),
                                                    "pkid",
                                                    true))
                                    + " records");
                });
    }

    private Map<Long, String> selection() {
        Map<Long, String> out = new TreeMap<>();
        grid.getSelectedItems().forEach(row -> out.put(id(row), (String) row.get("revision")));
        return out;
    }

    private void openEditor(Long id) {
        Map<String, Object> row = id == null ? defaults() : queries.read(resource, id);
        boolean editable = access.allowed(resource, id == null ? "add" : "change");
        if (resource == AdminResource.USERS
                && !access.actor().superuser()
                && (Boolean.TRUE.equals(row.get("is_staff"))
                        || Boolean.TRUE.equals(row.get("is_superuser")))) editable = false;
        final boolean mayEdit = editable;
        Dialog dialog = new Dialog();
        dialog.addClassName("admin-editor");
        dialog.setHeaderTitle(
                (id == null ? "Add " : editable ? "Change " : "View ") + resource.singular);
        dialog.setWidth("820px");
        dialog.setMaxWidth("96vw");
        dialog.setCloseOnOutsideClick(false);
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);
        if (id != null) {
            Span meta =
                    new Span(
                            "#"
                                    + id
                                    + " · Created "
                                    + display(row.get("created_at"))
                                    + " · Updated "
                                    + display(row.get("updated_at")));
            meta.addClassName("admin-muted");
            body.add(meta);
        }
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("580px", 2));
        Map<String, Supplier<Object>> values = new LinkedHashMap<>();
        Map<String, Component> components = new LinkedHashMap<>();
        for (var field : resource.fields) {
            Component component = field(field, row, values);
            if (component instanceof HasEnabled enabled) enabled.setEnabled(editable);
            if (resource == AdminResource.USERS
                    && (field.key().equals("is_staff")
                            || field.key().equals("is_superuser")
                            || (id != null && field.key().equals("is_active")))
                    && !access.actor().superuser()) ((HasEnabled) component).setEnabled(false);
            if (resource == AdminResource.PROFILES && id != null && field.key().equals("user_pkid"))
                ((HasEnabled) component).setEnabled(false);
            component.getElement().setAttribute("data-field", field.key());
            components.put(field.key(), component);
            form.add(component);
            if (field.kind() == AdminResource.Kind.AREA) form.setColspan(component, 2);
            if (field.kind() == AdminResource.Kind.PASSWORD) {
                PasswordField confirm = new PasswordField("Confirm password");
                confirm.setEnabled(editable);
                confirm.setMaxLength(128);
                values.put("password_confirmation", confirm::getValue);
                form.add(confirm);
                ((PasswordField) component)
                        .setHelperText(
                                id == null
                                        ? "At least 8 characters, including uppercase, lowercase"
                                                + " and a number."
                                        : "Leave blank to keep the current password.");
            }
        }
        if (resource == AdminResource.CONTENT_VIEWS) {
            @SuppressWarnings("unchecked")
            Select<String> type = (Select<String>) components.get("content_type");
            @SuppressWarnings("unchecked")
            ComboBox<AdminQueryService.Choice> content =
                    (ComboBox<AdminQueryService.Choice>) components.get("object_pkid");
            type.addValueChangeListener(
                    event -> {
                        content.clear();
                        content.setItems(
                                q ->
                                        queries
                                                .choices(
                                                        "post".equals(type.getValue())
                                                                ? "posts"
                                                                : "issues",
                                                        q.getFilter().orElse(""),
                                                        q.getOffset(),
                                                        q.getLimit())
                                                .stream());
                    });
        }
        if (access.actor().superuser()
                && (resource == AdminResource.USERS || resource == AdminResource.GROUPS)) {
            if (resource == AdminResource.USERS) {
                MultiSelectComboBox<AdminQueryService.Choice> groups =
                        new MultiSelectComboBox<>("Groups");
                groups.setItemLabelGenerator(AdminQueryService.Choice::label);
                groups.setItems(
                        q ->
                                queries
                                        .choices(
                                                "admin_groups",
                                                q.getFilter().orElse(""),
                                                q.getOffset(),
                                                q.getLimit())
                                        .stream());
                groups.setValue(
                        new LinkedHashSet<>(
                                longs(row.get("groups")).stream()
                                        .map(g -> queries.choice("admin_groups", g))
                                        .filter(Objects::nonNull)
                                        .toList()));
                groups.setEnabled(editable);
                values.put(
                        "groups",
                        () ->
                                groups.getValue().stream()
                                        .map(AdminQueryService.Choice::id)
                                        .toList());
                form.add(groups);
                form.setColspan(groups, 2);
            }
            MultiSelectComboBox<String> permissions =
                    new MultiSelectComboBox<>(
                            resource == AdminResource.USERS ? "Direct permissions" : "Permissions");
            permissions.setItems(AdminPermissions.all());
            permissions.setValue(new LinkedHashSet<>(strings(row.get("permissions"))));
            permissions.setEnabled(editable);
            permissions.setHelperText(
                    "Staff access is required. Superusers have every permission. Grants from all"
                            + " groups and direct permissions are combined.");
            values.put("permissions", permissions::getValue);
            form.add(permissions);
            form.setColspan(permissions, 2);
        }
        body.add(form);
        if (resource == AdminResource.PROFILES && id != null) {
            body.add(
                    new Paragraph(
                            "Distinct reporters: "
                                    + row.get("report_count")
                                    + " · Reputation: "
                                    + row.get("reputation")
                                    + " / 100. These values follow the report records."));
            Image avatar = new Image();
            avatar.setAlt("Current profile picture");
            avatar.setWidth("80px");
            avatar.setHeight("80px");
            avatar.getStyle().set("object-fit", "cover").set("border-radius", "50%");
            Runnable refreshAvatar =
                    () -> {
                        String url = Objects.toString(row.get("avatar"), "");
                        avatar.setVisible(!url.isBlank());
                        if (url.isBlank()) avatar.getElement().removeAttribute("src");
                        else avatar.setSrc(url);
                    };
            refreshAvatar.run();
            body.add(avatar);
            if (editable) {
                MemoryBuffer buffer = new MemoryBuffer();
                Upload upload = new Upload(buffer);
                upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp");
                upload.setMaxFiles(1);
                upload.setMaxFileSize(1024 * 1024);
                upload.setDropLabel(
                        new Span("Upload a profile picture · JPEG, PNG, GIF or WebP · up to 1 MB"));
                upload.addSucceededListener(
                        event ->
                                run(
                                        () -> {
                                            try {
                                                writes.uploadAvatar(
                                                        id,
                                                        (String) row.get("revision"),
                                                        new ImageUpload(
                                                                event.getFileName(),
                                                                event.getMIMEType(),
                                                                buffer.getInputStream()
                                                                        .readAllBytes()));
                                                row.putAll(queries.read(resource, id));
                                                refreshAvatar.run();
                                                Notification.show("Profile picture saved");
                                            } catch (IOException e) {
                                                throw new IllegalArgumentException(
                                                        "Could not read the image.");
                                            }
                                        }));
                upload.addFileRejectedListener(event -> Notification.show(event.getErrorMessage()));
                body.add(upload);
                Button clear =
                        new Button(
                                "Remove profile picture",
                                event ->
                                        run(
                                                () -> {
                                                    writes.clearAvatar(
                                                            id, (String) row.get("revision"));
                                                    row.putAll(queries.read(resource, id));
                                                    refreshAvatar.run();
                                                    Notification.show("Profile picture removed");
                                                }));
                body.add(clear);
            }
        }
        if (resource == AdminResource.USERS && id != null)
            body.add(
                    new Paragraph(
                            "Joined: "
                                    + display(row.get("date_joined"))
                                    + " · Last login: "
                                    + display(row.get("last_login"))));
        if (resource == AdminResource.POSTS && id != null)
            body.add(
                    new Paragraph(
                            "Upvotes: "
                                    + row.get("upvotes")
                                    + " · Downvotes: "
                                    + row.get("downvotes")
                                    + " · Views: "
                                    + row.get("total_views")
                                    + ". Vote totals are calculated from residents’ votes."));
        if (resource == AdminResource.ISSUES && id != null)
            body.add(
                    new Paragraph(
                            "Resolved on: "
                                    + display(row.get("resolved_on"))
                                    + " · Views: "
                                    + row.get("total_views")));
        if (id != null
                && (resource == AdminResource.POSTS || resource == AdminResource.ISSUES)
                && access.allowed(AdminResource.CONTENT_VIEWS, "view")) {
            var views = queries.contentViews(resource, id);
            Grid<Map<String, Object>> inline = new Grid<>();
            inline.addColumn(v -> display(v.get("user_label"))).setHeader("Viewer");
            inline.addColumn(v -> display(v.get("viewer_ip"))).setHeader("IP");
            inline.addColumn(v -> display(v.get("last_viewed"))).setHeader("Last viewed");
            inline.setItems(views);
            inline.setHeight("200px");
            body.add(new H4("Recent content views"), inline);
        }
        Span error = new Span();
        error.addClassName("admin-error");
        error.getElement().setAttribute("role", "alert");
        body.add(error);
        dialog.add(body);
        Button close = new Button("Close", event -> dialog.close());
        dialog.getFooter().add(close);
        if (id != null && access.allowed(resource, "delete")) {
            Button delete =
                    new Button(
                            "Delete",
                            event ->
                                    run(
                                            () ->
                                                    confirmDelete(
                                                            Map.of(
                                                                    id,
                                                                    (String) row.get("revision")),
                                                            dialog)));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            dialog.getFooter().add(delete);
        }
        if (mayEdit) {
            Button save =
                    new Button(
                            "Save",
                            event -> {
                                try {
                                    var input = new LinkedHashMap<String, Object>();
                                    values.forEach((key, value) -> input.put(key, value.get()));
                                    writes.save(resource, id, (String) row.get("revision"), input);
                                    dialog.close();
                                    refresh();
                                    Notification.show("Saved " + resource.singular);
                                } catch (Exception e) {
                                    error.setText(error(e));
                                }
                            });
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dialog.getFooter().add(save);
        }
        dialog.open();
    }

    private Component field(
            AdminResource.Field f, Map<String, Object> row, Map<String, Supplier<Object>> values) {
        Object value = row.get(f.key());
        switch (f.kind()) {
            case BOOLEAN -> {
                Checkbox c = new Checkbox(f.label());
                c.setValue(Boolean.TRUE.equals(value));
                values.put(f.key(), c::getValue);
                return c;
            }
            case CHOICE -> {
                Select<String> c = new Select<>();
                c.setLabel(f.label());
                c.setItems(f.choices());
                if (value != null) c.setValue(value.toString());
                values.put(f.key(), c::getValue);
                return c;
            }
            case RELATION -> {
                ComboBox<AdminQueryService.Choice> c = new ComboBox<>(f.label());
                c.setItemLabelGenerator(AdminQueryService.Choice::label);
                c.setClearButtonVisible(!f.required());
                c.setRequiredIndicatorVisible(f.required());
                Supplier<String> table =
                        () ->
                                "content".equals(f.reference())
                                        ? ("post".equals(values.get("content_type").get())
                                                ? "posts"
                                                : "issues")
                                        : f.reference();
                c.setItems(
                        q ->
                                queries
                                        .choices(
                                                table.get(),
                                                q.getFilter().orElse(""),
                                                q.getOffset(),
                                                q.getLimit())
                                        .stream());
                if (value != null)
                    c.setValue(queries.choice(table.get(), AdminRecordService.number(value)));
                values.put(f.key(), () -> c.getValue() == null ? null : c.getValue().id());
                return c;
            }
            case INTEGER -> {
                IntegerField c = new IntegerField(f.label());
                c.setMin(f.key().equals("rating") ? 1 : 0);
                if (f.key().equals("rating")) c.setMax(5);
                if (value != null) c.setValue(((Number) value).intValue());
                c.setRequiredIndicatorVisible(true);
                values.put(f.key(), c::getValue);
                return c;
            }
            case DATETIME -> {
                DateTimePicker c = new DateTimePicker(f.label());
                if (value instanceof Timestamp t)
                    c.setValue(LocalDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC));
                else c.setValue(LocalDateTime.now(ZoneOffset.UTC));
                values.put(f.key(), c::getValue);
                return c;
            }
            case AREA -> {
                TextArea c = new TextArea(f.label());
                c.setValue(Objects.toString(value, ""));
                c.setMinHeight("140px");
                c.setMaxLength(f.maxLength());
                c.setRequiredIndicatorVisible(f.required());
                values.put(f.key(), c::getValue);
                return c;
            }
            case PASSWORD -> {
                PasswordField c = new PasswordField(f.label());
                c.setMaxLength(128);
                values.put(f.key(), c::getValue);
                return c;
            }
            default -> {
                TextField c = new TextField(f.label());
                c.setValue(Objects.toString(value, ""));
                c.setMaxLength(f.maxLength());
                c.setRequiredIndicatorVisible(f.required());
                if (f.key().equals("email")) c.setAutocapitalize(Autocapitalize.NONE);
                values.put(f.key(), c::getValue);
                return c;
            }
        }
    }

    private Map<String, Object> defaults() {
        var values = new LinkedHashMap<String, Object>();
        for (var field : resource.fields) {
            if (field.kind() == AdminResource.Kind.CHOICE)
                values.put(field.key(), field.choices().getFirst());
            if (field.kind() == AdminResource.Kind.BOOLEAN) values.put(field.key(), false);
        }
        if (resource == AdminResource.USERS) values.put("is_active", true);
        if (resource == AdminResource.PROFILES) {
            values.put("gender", "other");
            values.put("country_of_origin", "IN");
            values.put("city_of_origin", "New Delhi");
            values.put("phone_number", "+91");
        }
        if (resource == AdminResource.APARTMENTS) values.put("floor", 0);
        if (resource == AdminResource.RATINGS) values.put("rating", 5);
        return values;
    }

    private void confirmDelete(Map<Long, String> selection, Dialog editor) {
        var preview = deletes.preview(resource, selection);
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm deletion");
        confirm.setWidth("580px");
        confirm.setMaxWidth("94vw");
        confirm.setCloseOnOutsideClick(false);
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.add(
                new Paragraph(
                        "Permanently delete these records and their related data? This cannot be"
                                + " undone."));
        preview.counts()
                .forEach(
                        (name, count) -> {
                            if (count > 0) body.add(new Span(name + ": " + count));
                        });
        preview.examples().forEach(s -> body.add(new Span(s)));
        preview.effects().forEach(s -> body.add(new Paragraph(s)));
        Span error = new Span();
        error.addClassName("admin-error");
        error.getElement().setAttribute("role", "alert");
        body.add(error);
        confirm.add(body);
        Button remove =
                new Button(
                        "Delete permanently",
                        event -> {
                            try {
                                deletes.delete(resource, selection, preview.fingerprint());
                                confirm.close();
                                if (editor != null) editor.close();
                                refresh();
                                Notification.show("Records deleted");
                            } catch (Exception e) {
                                error.setText(error(e));
                            }
                        });
        remove.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(new Button("Cancel", event -> confirm.close()), remove);
        confirm.open();
    }

    private void confirmStatus() {
        var selection = selection();
        String status = bulkStatus.getValue();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Update issue status");
        confirm.add(
                new Paragraph(
                        "Set "
                                + selection.size()
                                + " selected issues to "
                                + status.replace('_', ' ')
                                + "? Resolving issues sends a notification to their reporters."));
        confirm.getFooter()
                .add(
                        new Button("Cancel", e -> confirm.close()),
                        new Button(
                                "Confirm update",
                                e ->
                                        run(
                                                () -> {
                                                    writes.bulkIssueStatus(selection, status);
                                                    confirm.close();
                                                    refresh();
                                                    Notification.show("Issues updated");
                                                })));
        confirm.open();
    }

    private void openHistory(long id) {
        AdminHistoryView.openHistory(audit, resource, id);
    }

    private static long id(Map<String, Object> row) {
        return AdminRecordService.number(row.get("pkid"));
    }

    static String display(Object value) {
        if (value == null) return "—";
        if (value instanceof Boolean b) return b ? "Yes" : "No";
        return value.toString();
    }

    static String label(String key) {
        String label =
                key.replace("_label", "").replace("_pkid", "").replace("is_", "").replace('_', ' ');
        return label.substring(0, 1).toUpperCase() + label.substring(1);
    }

    private static List<Long> longs(Object value) {
        return value instanceof Collection<?> c
                ? c.stream().map(AdminRecordService::number).toList()
                : List.of();
    }

    private static List<String> strings(Object value) {
        return value instanceof Collection<?> c
                ? c.stream().map(Object::toString).toList()
                : List.of();
    }

    private static void run(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            Notification.show(error(e), 6500, Notification.Position.MIDDLE);
        }
    }

    static String error(Exception e) {
        if (e instanceof DataIntegrityViolationException)
            return "Could not save: a unique value is already in use, or a related record changed."
                    + " Review your entries and refresh.";
        if (e instanceof IllegalArgumentException || e instanceof AccessDeniedException)
            return e.getMessage();
        if (e instanceof ResponseStatusException status) return status.getReason();
        org.slf4j.LoggerFactory.getLogger(AdminCrudView.class)
                .error("Administration action failed", e);
        return "The action could not be completed. Refresh the page and try again.";
    }

    private record ImageUpload(String name, String contentType, byte[] bytes)
            implements MultipartFile {
        public String getName() {
            return "avatar";
        }

        public String getOriginalFilename() {
            return name;
        }

        public String getContentType() {
            return contentType;
        }

        public boolean isEmpty() {
            return bytes.length == 0;
        }

        public long getSize() {
            return bytes.length;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
