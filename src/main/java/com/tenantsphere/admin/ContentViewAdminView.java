package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "content-views", layout = AdminLayout.class)
@PageTitle("Content views | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ContentViewAdminView extends AdminCrudView {
    public ContentViewAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.CONTENT_VIEWS, queries, writes, deletes, permissions, audit);
    }
}
