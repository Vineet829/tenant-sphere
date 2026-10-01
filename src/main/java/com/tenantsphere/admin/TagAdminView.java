package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "tags", layout = AdminLayout.class)
@PageTitle("Tags | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class TagAdminView extends AdminCrudView {
    public TagAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.TAGS, queries, writes, deletes, permissions, audit);
    }
}
