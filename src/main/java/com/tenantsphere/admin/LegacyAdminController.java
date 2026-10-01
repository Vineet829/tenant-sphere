package com.tenantsphere.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Keep bookmarks to the former Django admin useful after switching backends. */
@Controller
public class LegacyAdminController {
    @GetMapping({"/supersecret", "/supersecret/", "/supersecret/**"})
    public String legacyAdmin() {
        return "redirect:/admin/";
    }
}
