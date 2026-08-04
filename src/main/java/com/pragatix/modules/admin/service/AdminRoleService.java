package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Role;
import com.pragatix.modules.authentication.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminRoleService {
    private static final Logger log = LoggerFactory.getLogger(AdminRoleService.class);

    private final RoleRepository roleRepository;

    public AdminRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll().stream()
                .filter(r -> r.getName().equals("ROLE_TEACHER") || r.getName().equals("ROLE_TRANSPORT"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Map<String, String> body) {
        String roleName = body.get("name");
        if (roleName == null || roleName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role name is required"));
        }
        String formattedName = roleName.trim().toUpperCase();
        if (!formattedName.startsWith("ROLE_")) {
            formattedName = "ROLE_" + formattedName;
        }
        if (roleRepository.existsByName(formattedName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role already exists"));
        }
        Role role = Role.builder().name(formattedName).build();
        Role saved = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Role created successfully", saved));
    }

}
