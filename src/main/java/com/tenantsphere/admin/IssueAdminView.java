package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "issues", layout = AdminLayout.class)
@PageTitle("Issues | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class IssueAdminView extends AdminCrudView {
    public IssueAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.ISSUES, queries, writes, deletes, permissions, audit);
    }
}
