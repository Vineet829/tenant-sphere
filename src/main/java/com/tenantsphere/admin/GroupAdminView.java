package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "groups", layout = AdminLayout.class)
@PageTitle("Groups | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class GroupAdminView extends AdminCrudView {
    public GroupAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.GROUPS, queries, writes, deletes, permissions, audit);
    }
}
