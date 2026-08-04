package com.pragatix.modules.superadmin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.superadmin.dto.YearAdminResponse;
import com.pragatix.modules.superadmin.dto.AssignAcademicYearRequest;
import com.pragatix.enums.AcademicYear;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;

    public SuperAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ResponseEntity<ApiResponse<List<YearAdminResponse>>> getYearAdmins() {
        List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");

        System.out.println("--- FETCHING YEAR ADMINS ---");
        List<YearAdminResponse> response = admins.stream()
                .filter(u -> u.getRoles().stream().noneMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName())))
                .map(u -> {
                    System.out.println("Admin : " + u.getUsername() + ", Academic Year : " + u.getAcademicYear());
                    return new YearAdminResponse(
                            u.getId(),
                            u.getFullName(),
                            u.getUsername(),
                            u.getAcademicYear(),
                            u.isActive());
                })
                .collect(Collectors.toList());
        System.out.println("----------------------------");
        return ResponseEntity.ok(ApiResponse.ok("Fetched Year Admins", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<YearAdminResponse>> assignAcademicYear(Long id,
            AssignAcademicYearRequest request) {
        User admin = userRepository.findById(id).orElse(null);
        if (admin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Admin user not found"));
        }

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (!isAdmin) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User is not a Year Admin"));
        }

        AcademicYear year = request.getAcademicYear();
        if (year == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Academic Year is required"));
        }

        admin.setAcademicYear(year);
        userRepository.save(admin);

        User savedUser = userRepository.findById(admin.getId()).orElse(null);
        System.out.println("--- DB UPDATE VERIFICATION ---");
        if (savedUser != null) {
            System.out.println("User ID: " + savedUser.getId());
            System.out.println("Username: " + savedUser.getUsername());
            System.out.println("Academic Year: " + savedUser.getAcademicYear());
        }
        System.out.println("------------------------------");

        YearAdminResponse resp = new YearAdminResponse(
                admin.getId(),
                admin.getFullName(),
                admin.getUsername(),
                admin.getAcademicYear(),
                admin.isActive());
        return ResponseEntity.ok(ApiResponse.ok("Academic Year assigned successfully", resp));
    }
}
