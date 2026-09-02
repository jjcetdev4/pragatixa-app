package jjcet.PragatiX.modules.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import jjcet.PragatiX.modules.superadmin.service.SuperAdminService;
import jjcet.PragatiX.modules.admin.service.AdminDashboardService;
import jjcet.PragatiX.modules.admin.service.AdminUserService;

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
        jjcet.PragatiX.modules.superadmin.dto.CreateYearAdminRequest req = new jjcet.PragatiX.modules.superadmin.dto.CreateYearAdminRequest();
        req.setUsername("testadmin_bug");
        req.setFullName("Test Admin Bug");
        req.setEmail("testbug@example.com");
        req.setPhone("1234567890");
        req.setAssignedYearId(1L);
        req.setActive(true);
        return superAdminService.createYearAdmin(req);
    }

    @GetMapping("/admins")
    public Object getAdmins() {
        return superAdminService.getYearAdmins();
    }

    @GetMapping("/users")
    public Object getUsers() {
        return adminUserService.getAllUsers(null, null);
    }
}
