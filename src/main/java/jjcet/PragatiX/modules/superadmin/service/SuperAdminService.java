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
    private final jjcet.PragatiX.repository.YearRepository yearRepository;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public SuperAdminService(UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            jjcet.PragatiX.modules.authentication.repository.RoleRepository roleRepository,
            jjcet.PragatiX.repository.ActivityAssignmentRepository activityAssignmentRepository,
            jjcet.PragatiX.repository.YearRepository yearRepository,
            jjcet.PragatiX.modules.audit.service.AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.yearRepository = yearRepository;
        this.auditService = auditService;
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
                    System.out.println("Admin : " + u.getUsername() + ", Assigned Year : " + (u.getAssignedYear() != null ? u.getAssignedYear().getYearName() : "None"));
                    return new YearAdminResponse(
                            u.getId(),
                            u.getFullName(),
                            u.getUsername(),
                            u.getAssignedYear() != null ? u.getAssignedYear().getId() : null,
                            u.getAssignedYear() != null ? u.getAssignedYear().getYearName() : null,
                            u.getEmail(),
                            u.getPhone(),
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

        if (request.getAssignedYearId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Assigned Academic Year is required"));
        }
        jjcet.PragatiX.entity.Year year = yearRepository.findById(request.getAssignedYearId()).orElse(null);
        if (year == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Selected Academic Year does not exist"));
        }

        if (userRepository.existsByAssignedYearIdAndRolesName(request.getAssignedYearId(), "ROLE_ADMIN")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ApiResponse.error("Year " + year.getYearName() + " is already assigned to another Admin."));
        }

        jjcet.PragatiX.entity.Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));

        java.util.Set<jjcet.PragatiX.entity.Role> roles = new java.util.HashSet<>();
        roles.add(adminRole);

        jjcet.PragatiX.enums.AcademicYear mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.fromString(year.getYearName());
        if (mappedAcademicYear == null && year.getYearNo() != null) {
            if (year.getYearNo() == 1) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
            else if (year.getYearNo() == 2) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.SECOND_YEAR;
            else if (year.getYearNo() == 3) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.THIRD_YEAR;
            else if (year.getYearNo() == 4) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.FOURTH_YEAR;
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(roles)
                .assignedYear(year)
                .year(year.getYearName())
                .academicYear(mappedAcademicYear)
                .active(request.isActive())
                .build();

        User savedAdmin;
        try {
            savedAdmin = userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username or Email is already taken."));
        }

        try {
            auditService.log(
                jjcet.PragatiX.enums.AuditAction.CREATE,
                jjcet.PragatiX.enums.AuditModule.ADMIN,
                "ADMIN",
                savedAdmin.getId(),
                "Created admin " + savedAdmin.getUsername() + " (" + savedAdmin.getFullName() + ")"
            );
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }

        YearAdminResponse resp = new YearAdminResponse(
                savedAdmin.getId(),
                savedAdmin.getFullName(),
                savedAdmin.getUsername(),
                savedAdmin.getAssignedYear() != null ? savedAdmin.getAssignedYear().getId() : null,
                savedAdmin.getAssignedYear() != null ? savedAdmin.getAssignedYear().getYearName() : null,
                savedAdmin.getEmail(),
                savedAdmin.getPhone(),
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

        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), admin.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), admin.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered"));
            }
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

        if (request.getAssignedYearId() != null) {
            jjcet.PragatiX.entity.Year year = yearRepository.findById(request.getAssignedYearId()).orElse(null);
            if (year == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Selected Academic Year does not exist"));
            }

            if (userRepository.existsByAssignedYearIdAndRolesNameAndIdNot(request.getAssignedYearId(), "ROLE_ADMIN", admin.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ApiResponse.error("Year " + year.getYearName() + " is already assigned to another Admin."));
            }

            jjcet.PragatiX.enums.AcademicYear mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.fromString(year.getYearName());
            if (mappedAcademicYear == null && year.getYearNo() != null) {
                if (year.getYearNo() == 1) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
                else if (year.getYearNo() == 2) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.SECOND_YEAR;
                else if (year.getYearNo() == 3) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.THIRD_YEAR;
                else if (year.getYearNo() == 4) mappedAcademicYear = jjcet.PragatiX.enums.AcademicYear.FOURTH_YEAR;
            }

            admin.setAssignedYear(year);
            admin.setYear(year.getYearName());
            admin.setAcademicYear(mappedAcademicYear);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Assigned Academic Year is required"));
        }

        try {
            userRepository.save(admin);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username or Email is already taken."));
        }

        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("username", admin.getUsername());
        newValues.put("fullName", admin.getFullName());
        newValues.put("email", admin.getEmail());
        if (admin.getAssignedYear() != null) newValues.put("assignedYearId", admin.getAssignedYear().getId());

        try {
            auditService.log(
                jjcet.PragatiX.enums.AuditAction.UPDATE,
                jjcet.PragatiX.enums.AuditModule.ADMIN,
                "ADMIN",
                admin.getId(),
                "Updated admin " + admin.getUsername(),
                oldValues,
                newValues
            );
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }

        YearAdminResponse resp = new YearAdminResponse(
                admin.getId(),
                admin.getFullName(),
                admin.getUsername(),
                admin.getAssignedYear() != null ? admin.getAssignedYear().getId() : null,
                admin.getAssignedYear() != null ? admin.getAssignedYear().getYearName() : null,
                admin.getEmail(),
                admin.getPhone(),
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
        
        try {
            auditService.log(
                jjcet.PragatiX.enums.AuditAction.PERMANENT_DELETE,
                jjcet.PragatiX.enums.AuditModule.ADMIN,
                "ADMIN",
                id,
                "Permanently deleted admin " + admin.getUsername()
            );
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Year Admin deleted successfully", null));
    }
}
