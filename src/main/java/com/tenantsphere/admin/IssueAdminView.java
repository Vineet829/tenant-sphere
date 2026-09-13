package com.tenantsphere.admin;

import com.tenantsphere.common.ContentView;
import com.tenantsphere.common.ContentViewRepository;
import com.tenantsphere.issue.Issue;
import com.tenantsphere.issue.IssueRepository;
import com.tenantsphere.issue.IssueService;
import com.tenantsphere.issue.IssueStatus;
import com.tenantsphere.issue.Priority;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@Route(value = "admin/issues", layout = AdminLayout.class)
@PageTitle("Issues | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class IssueAdminView extends AdminGridView<Issue> {

    private final IssueRepository issueRepository;
    private final ContentViewRepository contentViewRepository;
    private final UserRepository userRepository;

    public IssueAdminView(
            IssueRepository issueRepository,
            ContentViewRepository contentViewRepository,
            UserRepository userRepository) {
        super("Issues", Issue.class);
        this.issueRepository = issueRepository;
        this.contentViewRepository = contentViewRepository;
        this.userRepository = userRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(Issue::getId).setHeader("id").setAutoWidth(true);
        grid.addColumn(issue -> issue.getApartment().getUnitNumber())
                .setHeader("Apartment").setAutoWidth(true).setSortable(true);
        grid.addColumn(issue -> issue.getReportedBy().getFullName())
                .setHeader("Reported by").setAutoWidth(true);
        grid.addColumn(issue -> issue.getAssignedTo() == null
                        ? "-" : issue.getAssignedTo().getFullName())
                .setHeader("Assigned to").setAutoWidth(true);
        grid.addColumn(issue -> issue.getStatus().getValue())
                .setHeader("Status").setAutoWidth(true).setSortable(true);
        grid.addColumn(issue -> issue.getPriority().getValue())
                .setHeader("Priority").setAutoWidth(true).setSortable(true);
        grid.addColumn(this::totalViews).setHeader("Total Views").setAutoWidth(true);

        grid.setItemDetailsRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(
                this::contentViewInline));

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openEditor(event.getValue());
            }
        });

        onSearch(this::refresh);
        refresh("");
    }

    private long totalViews(Issue issue) {
        return contentViewRepository.countByContentTypeAndObjectPkid(
                IssueService.CONTENT_TYPE, issue.getPkid());
    }

    private VerticalLayout contentViewInline(Issue issue) {
        List<ContentView> views = contentViewRepository
                .findByContentTypeAndObjectPkidOrderByLastViewedDesc(
                        IssueService.CONTENT_TYPE, issue.getPkid());

        Grid<ContentView> inline = new Grid<>(ContentView.class, false);
        inline.addColumn(view -> view.getUser() == null ? "Anonymous" : view.getUser().getUsername())
                .setHeader("User").setAutoWidth(true);
        inline.addColumn(ContentView::getViewerIp).setHeader("Viewer IP").setAutoWidth(true);
        inline.addColumn(ContentView::getCreatedAt).setHeader("Created at").setAutoWidth(true);
        inline.setItems(views);
        inline.setAllRowsVisible(true);

        VerticalLayout wrapper = new VerticalLayout(new H4("Content views"), inline);
        wrapper.setPadding(false);
        return wrapper;
    }

    private void refresh(String term) {
        List<Issue> issues = issueRepository.findAllBy().stream()
                .filter(issue -> term.isBlank()
                        || issue.getApartment().getUnitNumber().toLowerCase().contains(term.toLowerCase())
                        || issue.getReportedBy().getFirstName().toLowerCase().contains(term.toLowerCase())
                        || issue.getTitle().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        grid.setItems(issues);
    }

    private void openEditor(Issue issue) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change issue");
        dialog.setWidth("640px");

        TextField title = new TextField("Title");
        title.setValue(issue.getTitle());
        TextArea description = new TextArea("Description");
        description.setValue(issue.getDescription());

        Select<IssueStatus> status = new Select<>();
        status.setLabel("Status");
        status.setItems(IssueStatus.values());
        status.setItemLabelGenerator(IssueStatus::getValue);
        status.setValue(issue.getStatus());

        Select<Priority> priority = new Select<>();
        priority.setLabel("Priority");
        priority.setItems(Priority.values());
        priority.setItemLabelGenerator(Priority::getValue);
        priority.setValue(issue.getPriority());

        ComboBox<User> assignedTo = new ComboBox<>("Assigned to");
        assignedTo.setItems(query -> userRepository.findAll().stream()
                .filter(user -> query.getFilter()
                        .map(filter -> user.getUsername().toLowerCase().contains(filter.toLowerCase()))
                        .orElse(true))
                .skip(query.getOffset())
                .limit(query.getLimit()));
        assignedTo.setItemLabelGenerator(User::getFullName);
        assignedTo.setClearButtonVisible(true);
        assignedTo.setValue(issue.getAssignedTo());

        FormLayout form = new FormLayout(title, description, status, priority, assignedTo);
        dialog.add(form);

        Button save = new Button("Save", event -> {
            issue.setTitle(title.getValue());
            issue.setDescription(description.getValue());
            issue.setStatus(status.getValue());
            issue.setPriority(priority.getValue());
            issue.setAssignedTo(assignedTo.getValue());
            issueRepository.save(issue);
            Notification.show("Saved issue");
            dialog.close();
            refresh("");
        });
        Button cancel = new Button("Cancel", event -> dialog.close());
        dialog.getFooter().add(new HorizontalLayout(cancel, save));

        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                grid.deselectAll();
            }
        });
        dialog.open();
    }
}
