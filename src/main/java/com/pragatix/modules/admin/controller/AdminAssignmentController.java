package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;
import jjcet.PragatiX.modules.admin.service.*;
import jjcet.PragatiX.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminAssignmentController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminAssignmentController {
    private static final Logger log = LoggerFactory.getLogger(AdminAssignmentController.class);

    private final AdminAssignmentService adminAssignmentService;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService) {
        this.adminAssignmentService = adminAssignmentService;
    }

}
