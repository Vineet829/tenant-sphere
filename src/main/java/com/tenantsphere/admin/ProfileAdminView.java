package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "profiles", layout = AdminLayout.class)
@PageTitle("Profiles | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ProfileAdminView extends AdminCrudView {
    public ProfileAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.PROFILES, queries, writes, deletes, permissions, audit);
    }
}
