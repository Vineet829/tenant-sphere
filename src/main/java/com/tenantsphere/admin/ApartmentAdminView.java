package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "apartments", layout = AdminLayout.class)
@PageTitle("Apartments | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ApartmentAdminView extends AdminCrudView {
    public ApartmentAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.APARTMENTS, queries, writes, deletes, permissions, audit);
    }
}
