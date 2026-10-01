package com.tenantsphere.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "posts", layout = AdminLayout.class)
@PageTitle("Posts | TenantSphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class PostAdminView extends AdminCrudView {
    public PostAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.POSTS, queries, writes, deletes, permissions, audit);
    }
}
