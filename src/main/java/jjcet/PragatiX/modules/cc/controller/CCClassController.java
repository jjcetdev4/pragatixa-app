package jjcet.PragatiX.modules.cc.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.cc.service.CCActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cc")
@Tag(name = "CCClassController", description = "Class Coordinator Class Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CCClassController {

    private final CCActivityService ccActivityService;

    public CCClassController(CCActivityService ccActivityService) {
        this.ccActivityService = ccActivityService;
    }

    @GetMapping("/class-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get class details (Department, Year, Section) for the logged-in Class Coordinator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCCClassDetails() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getCCClassDetails(username);
    }
}
