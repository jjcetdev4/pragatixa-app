package com.pragatix.modules.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.pragatix.modules.superadmin.service.SuperAdminService;
import com.pragatix.modules.admin.service.AdminDashboardService;
import com.pragatix.modules.admin.service.AdminUserService;

@RestController
@RequestMapping("/api/test-bug")
public class TestBugController {

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/create")
    public Object create() {
        com.pragatix.modules.superadmin.dto.CreateYearAdminRequest req = new com.pragatix.modules.superadmin.dto.CreateYearAdminRequest();
        req.setUsername("testadmin_bug");
        req.setPassword("password");
        req.setFullName("Test Admin Bug");
        req.setEmail("testbug@example.com");
        req.setPhone("1234567890");
        req.setAcademicYear(com.pragatix.enums.AcademicYear.FIRST_YEAR);
        req.setActive(true);
        return superAdminService.createYearAdmin(req);
    }

    @GetMapping("/admins")
    public Object getAdmins() {
        return superAdminService.getYearAdmins();
    }

    @GetMapping("/users")
    public Object getUsers() {
        return adminUserService.getAllUsers(null);
    }
}
