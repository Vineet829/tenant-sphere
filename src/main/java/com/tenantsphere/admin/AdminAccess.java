package com.tenantsphere.admin;

import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.user.User;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

final class AdminAccess {
    private AdminAccess() {}

    static User requireStaff() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails details) {
            User user = details.getUser();
            if (user.isActive() && (user.isStaff() || user.isSuperuser())) return user;
        }
        throw new AccessDeniedException("Staff access is required.");
    }
}
