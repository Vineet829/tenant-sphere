package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "reports", layout = AdminLayout.class)
@PageTitle("Reports | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ReportAdminView extends AdminCrudView {
    public ReportAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.REPORTS, queries, writes, deletes, permissions, audit);
    }
}
