package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = AdminLayout.class)
@PageTitle("Users | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class UserAdminView extends AdminCrudView {
    public UserAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.USERS, queries, writes, deletes, permissions, audit);
    }
}
