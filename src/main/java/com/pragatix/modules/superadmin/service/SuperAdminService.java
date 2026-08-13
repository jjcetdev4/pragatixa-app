package jjcet.PragatiX.modules.superadmin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.superadmin.dto.YearAdminResponse;
import jjcet.PragatiX.modules.superadmin.dto.AssignAcademicYearRequest;
import jjcet.PragatiX.enums.AcademicYear;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final jjcet.PragatiX.modules.authentication.repository.RoleRepository roleRepository;
    private final jjcet.PragatiX.repository.ActivityAssignmentRepository activityAssignmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SuperAdminService(UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            jjcet.PragatiX.modules.authentication.repository.RoleRepository roleRepository,
            jjcet.PragatiX.repository.ActivityAssignmentRepository activityAssignmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
    }

    public ResponseEntity<ApiResponse<Void>> refreshDbCache() {
        entityManager.clear();
        try {
            if (entityManager.getEntityManagerFactory().getCache() != null) {
                entityManager.getEntityManagerFactory().getCache().evictAll();
            }
        } catch (Exception e) {
            System.err.println("Failed to evict L2 cache: " + e.getMessage());
        }
        System.out.println("--- DB CACHE REFRESHED ---");
        return ResponseEntity.ok(ApiResponse.ok("Database cache refreshed successfully", null));
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
    public ResponseEntity<ApiResponse<YearAdminResponse>> createYearAdmin(
            jjcet.PragatiX.modules.superadmin.dto.CreateYearAdminRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered"));
        }

        jjcet.PragatiX.entity.Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));

        java.util.Set<jjcet.PragatiX.entity.Role> roles = new java.util.HashSet<>();
        roles.add(adminRole);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(roles)
                .academicYear(request.getAcademicYear())
                .active(request.isActive())
                .build();

        User savedAdmin = userRepository.save(user);

        YearAdminResponse resp = new YearAdminResponse(
                savedAdmin.getId(),
                savedAdmin.getFullName(),
                savedAdmin.getUsername(),
                savedAdmin.getAcademicYear(),
                savedAdmin.isActive());

        return ResponseEntity.ok(ApiResponse.ok("Year Admin created successfully", resp));
    }

    @Transactional
    public ResponseEntity<ApiResponse<YearAdminResponse>> updateYearAdmin(Long id,
            jjcet.PragatiX.modules.superadmin.dto.UpdateYearAdminRequest request) {
        User admin = userRepository.findById(id).orElse(null);
        if (admin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Admin user not found"));
        }

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (!isAdmin) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User is not a Year Admin"));
        }

        // Update fields
        admin.setFullName(request.getFullName());
        admin.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setActive(request.isActive());

        // It's possible academic year is not assigned yet
        if (request.getAcademicYear() != null) {
            admin.setAcademicYear(request.getAcademicYear());
        } else {
            admin.setAcademicYear(null);
        }

        userRepository.save(admin);

        YearAdminResponse resp = new YearAdminResponse(
                admin.getId(),
                admin.getFullName(),
                admin.getUsername(),
                admin.getAcademicYear(),
                admin.isActive());
        return ResponseEntity.ok(ApiResponse.ok("Year Admin updated successfully", resp));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteYearAdmin(Long id) {
        User admin = userRepository.findById(id).orElse(null);
        if (admin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Admin user not found"));
        }

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        boolean isSuperAdmin = admin.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));

        if (!isAdmin || isSuperAdmin) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete this user via this endpoint"));
        }

        // Fetch current super admin to re-assign any activity assignments
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentSuperAdmin = userRepository.findByUsername(currentUsername).orElse(null);

        if (currentSuperAdmin != null) {
            List<jjcet.PragatiX.entity.ActivityAssignment> assignments = activityAssignmentRepository
                    .findByAssignedById(id);
            for (jjcet.PragatiX.entity.ActivityAssignment assignment : assignments) {
                assignment.setAssignedBy(currentSuperAdmin);
            }
            activityAssignmentRepository.saveAll(assignments);
        }

        userRepository.delete(admin);
        return ResponseEntity.ok(ApiResponse.ok("Year Admin deleted successfully", null));
    }
}
