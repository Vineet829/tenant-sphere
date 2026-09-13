package com.tenantsphere.admin;

import com.tenantsphere.common.ContentView;
import com.tenantsphere.common.ContentViewRepository;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@Route(value = "admin/content-views", layout = AdminLayout.class)
@PageTitle("Content views | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ContentViewAdminView extends AdminGridView<ContentView> {

    private final ContentViewRepository contentViewRepository;

    public ContentViewAdminView(ContentViewRepository contentViewRepository) {
        super("Content views", ContentView.class);
        this.contentViewRepository = contentViewRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(ContentView::getContentType).setHeader("Content type").setAutoWidth(true).setSortable(true);
        grid.addColumn(ContentView::getObjectPkid).setHeader("Object").setAutoWidth(true);
        grid.addColumn(view -> view.getUser() == null ? "Anonymous" : view.getUser().getUsername())
                .setHeader("User").setAutoWidth(true);
        grid.addColumn(ContentView::getViewerIp).setHeader("Viewer IP").setAutoWidth(true);
        grid.addColumn(ContentView::getCreatedAt).setHeader("Created at").setAutoWidth(true).setSortable(true);

        onSearch(this::refresh);
        refresh("");
    }

    private void refresh(String term) {
        List<ContentView> views = contentViewRepository.findAllBy().stream()
                .filter(view -> term.isBlank()
                        || view.getContentType().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        grid.setItems(views);
    }
}
