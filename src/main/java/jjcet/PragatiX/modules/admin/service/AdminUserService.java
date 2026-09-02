package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.authentication.dto.request.CreateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.request.UpdateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.response.UserResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Role;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.modules.authentication.repository.RoleRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.entity.SubRole;
import jjcet.PragatiX.modules.authentication.repository.SubRoleRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.repository.YearRepository;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.entity.Year;
import jjcet.PragatiX.enums.AcademicYear;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import jjcet.PragatiX.modules.admin.service.*;
import jjcet.PragatiX.modules.admin.mapper.*;

@Service
public class AdminUserService {
    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final SectionRepository sectionRepository;
    private final SubRoleRepository subRoleRepository;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;
    private final YearRepository yearRepository;
    private final jjcet.PragatiX.repository.DisciplineLogRepository disciplineLogRepository;
    private final jjcet.PragatiX.repository.XpTransactionRepository xpTransactionRepository;
    private final jjcet.PragatiX.modules.faculty.repository.FacultyRepository facultyRepository;

    public AdminUserService(DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
            RoleRepository roleRepository, SectionRepository sectionRepository, SubRoleRepository subRoleRepository,
            UserRepository userRepository, AdminMapper adminMapper,
            jjcet.PragatiX.modules.audit.service.AuditService auditService,
            YearRepository yearRepository, jjcet.PragatiX.repository.DisciplineLogRepository disciplineLogRepository,
            jjcet.PragatiX.repository.XpTransactionRepository xpTransactionRepository,
            jjcet.PragatiX.modules.faculty.repository.FacultyRepository facultyRepository) {
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.sectionRepository = sectionRepository;
        this.subRoleRepository = subRoleRepository;
        this.userRepository = userRepository;
        this.adminMapper = adminMapper;
        this.auditService = auditService;
        this.yearRepository = yearRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.facultyRepository = facultyRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(Long departmentId, String keyword) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.isActive())
                .collect(Collectors.toList());
        if (departmentId != null) {
            users = users.stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(departmentId))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase().trim();
            users = users.stream()
                    .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerKeyword)) ||
                            (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerKeyword)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerKeyword)) ||
                            (u.getPhone() != null && u.getPhone().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }
        List<UserResponse> responses = users.stream()
                .map(adminMapper::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists as username"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered"));
        }

        boolean isCC = false;
        boolean isHOD = false;

        if (request.getSubRoles() != null) {
            for (String subRole : request.getSubRoles()) {
                String upper = subRole.trim().toUpperCase();
                if (upper.equals("CC")) {
                    isCC = true;
                } else if (upper.equals("HOD")) {
                    isHOD = true;
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Sub-role '" + subRole
                            + "' is not permitted for new teachers. Only HOD and CC are allowed."));
                }
            }
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            if (department == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Department not found"));
            }
        }

        boolean supportsSections = department != null && Boolean.TRUE.equals(department.getSupportsSections());

        // Section is optional for CC — allow CC with or without section
        if (!isCC && request.getSectionId() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section can only be assigned to CC."));
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                Role role = roleRepository.findByName(rName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + rName));
                roles.add(role);
            }
        } else {
            Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_TEACHER not found"));
            roles.add(teacherRole);
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        AcademicYear academicYearEnum = AcademicYear.fromString(request.getYear());
        Year assignedYear = null;
        if (academicYearEnum != null) {
            Byte yearNo = null;
            switch (academicYearEnum) {
                case FIRST_YEAR:
                    yearNo = 1;
                    break;
                case SECOND_YEAR:
                    yearNo = 2;
                    break;
                case THIRD_YEAR:
                    yearNo = 3;
                    break;
                case FOURTH_YEAR:
                    yearNo = 4;
                    break;
            }
            if (yearNo != null) {
                assignedYear = yearRepository.findByYearNo(yearNo).orElse(null);
            }
        }

        String conflictError = validateHodAndCcUniqueness(null, isHOD, isCC, department, academicYearEnum,
                request.getYear(), section);
        if (conflictError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(conflictError));
        }

        User user = User.builder()
                .username(request.getEmail())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .department(department)
                .roles(roles)
                .subRoles(this.resolveSubRoles(request.getSubRoles(), roles))
                .section(section)
                .year(request.getYear())
                .active(true)
                .build();

        user.setAcademicYear(academicYearEnum);
        user.setAssignedYear(assignedYear);

        User saved = userRepository.save(user);

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.CREATE,
                jjcet.PragatiX.enums.AuditModule.USER,
                "USER",
                saved.getId(),
                "Created user " + saved.getUsername()
                        + (saved.getFullName() != null ? " (" + saved.getFullName() + ")" : ""));

        log.debug("Admin created new user: {}", saved.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", adminMapper.toUserResponse(saved)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found with ID: " + id));
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Email already registered by another user");
            }
        });

        boolean isCC = false;
        boolean isHOD = false;

        if (request.getSubRoles() != null) {
            for (String subRole : request.getSubRoles()) {
                String upper = subRole.trim().toUpperCase();
                if (upper.equals("CC")) {
                    isCC = true;
                } else if (upper.equals("HOD")) {
                    isHOD = true;
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse
                            .error("Sub-role '" + subRole + "' is not permitted. Only HOD and CC are allowed."));
                }
            }
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            if (department == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Department not found"));
            }
        }

        boolean supportsSections = department != null && Boolean.TRUE.equals(department.getSupportsSections());

        // Section is optional for CC — allow CC with or without section
        if (!isCC && request.getSectionId() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section can only be assigned to CC."));
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                Role role = roleRepository.findByName(rName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + rName));
                roles.add(role);
            }
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDepartment(department);
        user.setSection(section);
        user.setYear(request.getYear());

        AcademicYear academicYearEnum = AcademicYear.fromString(request.getYear());
        Year assignedYear = null;
        if (academicYearEnum != null) {
            Byte yearNo = null;
            switch (academicYearEnum) {
                case FIRST_YEAR:
                    yearNo = 1;
                    break;
                case SECOND_YEAR:
                    yearNo = 2;
                    break;
                case THIRD_YEAR:
                    yearNo = 3;
                    break;
                case FOURTH_YEAR:
                    yearNo = 4;
                    break;
            }
            if (yearNo != null) {
                assignedYear = yearRepository.findByYearNo(yearNo).orElse(null);
            }
        }

        if (request.isActive()) {
            String conflictError = validateHodAndCcUniqueness(id, isHOD, isCC, department, academicYearEnum,
                    request.getYear(), section);
            if (conflictError != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(conflictError));
            }
        }

        user.setAcademicYear(academicYearEnum);
        user.setAssignedYear(assignedYear);

        user.getRoles().clear();
        if (roles != null) {
            user.getRoles().addAll(roles);
        }

        user.getSubRoles().clear();
        if (request.getSubRoles() != null) {
            user.getSubRoles().addAll(this.resolveSubRoles(request.getSubRoles(), roles));
        }

        user.setActive(request.isActive());

        User saved = userRepository.save(user);

        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("username", saved.getUsername());
        newValues.put("fullName", saved.getFullName());
        newValues.put("email", saved.getEmail());
        if (saved.getDepartment() != null)
            newValues.put("departmentId", saved.getDepartment().getId());

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.UPDATE,
                jjcet.PragatiX.enums.AuditModule.USER,
                "USER",
                saved.getId(),
                "Updated user " + saved.getUsername(),
                oldValues,
                newValues);

        log.debug("Admin updated user: {}", saved.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", adminMapper.toUserResponse(saved)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }

        user.setDeleted(true);
        user.setActive(false);
        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setPermanentDeleteAt(java.time.LocalDateTime.now().plusDays(30));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            user.setDeletedBy(auth.getName());
        }

        userRepository.save(user);

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.DELETE,
                jjcet.PragatiX.enums.AuditModule.USER,
                "USER",
                user.getId(),
                "Moved user " + user.getUsername() + " to Recycle Bin");

        log.debug("Admin soft deleted user with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("User moved to Recycle Bin", null));
    }

    public Set<SubRole> resolveSubRoles(Set<String> subRoleNames, Set<Role> roles) {
        if (subRoleNames == null)
            return new HashSet<>();
        Role defaultRole = roles != null && !roles.isEmpty() ? roles.iterator().next() : null;
        Set<SubRole> subRoles = new HashSet<>();
        for (String name : subRoleNames) {
            SubRole sr = subRoleRepository.findByName(name)
                    .orElseGet(() -> subRoleRepository.save(
                            SubRole.builder().name(name).role(defaultRole).build()));
            subRoles.add(sr);
        }
        return subRoles;
    }

    private String validateHodAndCcUniqueness(Long targetUserId, boolean isHOD, boolean isCC, Department department,
            AcademicYear academicYearEnum, String yearStr, Section section) {
        if (isHOD) {
            if (department == null) {
                return "Department is required for HOD.";
            }
            List<User> existingHods = userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted() && u.isActive())
                    .filter(u -> (targetUserId == null || !u.getId().equals(targetUserId)))
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(department.getId()))
                    .filter(u -> u.getSubRoles().stream().anyMatch(
                            sr -> "HOD".equalsIgnoreCase(sr.getName()) || "ROLE_HOD".equalsIgnoreCase(sr.getName())))
                    .collect(Collectors.toList());
            if (!existingHods.isEmpty()) {
                String existingName = existingHods.get(0).getFullName() != null
                        && !existingHods.get(0).getFullName().trim().isEmpty()
                                ? existingHods.get(0).getFullName().trim()
                                : existingHods.get(0).getEmail();
                return "Department '" + department.getName() + "' already has an assigned HOD: " + existingName;
            }
        }

        if (isCC) {
            if (department == null) {
                return "Department is required for Class Coordinator (CC).";
            }
            if (academicYearEnum == null && (yearStr == null || yearStr.trim().isEmpty())) {
                return "Year is required for Class Coordinator (CC).";
            }

            final AcademicYear targetYearEnum = academicYearEnum;
            final String targetYearStr = yearStr;
            final Section targetSection = section;

            List<User> existingCcs = userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted() && u.isActive())
                    .filter(u -> (targetUserId == null || !u.getId().equals(targetUserId)))
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(department.getId()))
                    .filter(u -> u.getSubRoles().stream()
                            .anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName())
                                    || "ROLE_CC".equalsIgnoreCase(sr.getName())
                                    || "CLASS_COORDINATOR".equalsIgnoreCase(sr.getName())))
                    .filter(u -> isMatchingUserYear(u, targetYearEnum, targetYearStr))
                    .filter(u -> isMatchingUserSection(u, targetSection))
                    .collect(Collectors.toList());
            if (!existingCcs.isEmpty()) {
                String existingName = existingCcs.get(0).getFullName() != null
                        && !existingCcs.get(0).getFullName().trim().isEmpty()
                                ? existingCcs.get(0).getFullName().trim()
                                : existingCcs.get(0).getEmail();
                String secName = (targetSection != null) ? " Section " + targetSection.getSectionName() : "";
                String yrName = (targetYearEnum != null) ? " " + targetYearEnum.name()
                        : (targetYearStr != null ? " Year " + targetYearStr : "");
                return "A Class Coordinator (CC) is already assigned for " + department.getName() + yrName + secName
                        + ": " + existingName;
            }
        }
        return null;
    }

    private boolean isMatchingUserYear(User u, AcademicYear targetEnum, String targetYearStr) {
        if (targetEnum != null && u.getAcademicYear() != null) {
            if (u.getAcademicYear() == targetEnum)
                return true;
        }
        String uYear = u.getYear() != null ? u.getYear()
                : (u.getAcademicYear() != null ? u.getAcademicYear().name() : null);
        String targetYear = targetYearStr != null ? targetYearStr : (targetEnum != null ? targetEnum.name() : null);
        return jjcet.PragatiX.admin.service.TeamValidationService.isMatchingYear(uYear, targetYear);
    }

    private boolean isMatchingUserSection(User u, Section targetSection) {
        if (targetSection == null) {
            return u.getSection() == null;
        }
        return u.getSection() != null && u.getSection().getId().equals(targetSection.getId());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getTeacherPointsHistory(Long teacherId) {
        User user = userRepository.findById(teacherId).orElse(null);
        if (user == null) {
            jjcet.PragatiX.entity.Faculty faculty = facultyRepository.findById(teacherId).orElse(null);
            if (faculty != null) {
                user = faculty.getUser();
            }
        }
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Teacher not found"));
        }

        List<jjcet.PragatiX.entity.DisciplineLog> disciplineLogs = disciplineLogRepository
                .findByTeacherUserId(user.getId());
        List<jjcet.PragatiX.entity.XpTransaction> xpTransactions = xpTransactionRepository
                .findByApprovedByNameOrUsername(user.getFullName(), user.getUsername());

        List<java.util.Map<String, Object>> historyList = new java.util.ArrayList<>();
        Set<String> uniqueStudents = new HashSet<>();
        int totalPositive = 0;
        int totalPenalty = 0;

        Map<String, Map<String, Object>> groupDlMap = new LinkedHashMap<>();
        Map<String, Map<String, Object>> groupXpMap = new LinkedHashMap<>();

        // 1. Process Discipline Logs
        if (disciplineLogs != null) {
            for (jjcet.PragatiX.entity.DisciplineLog dl : disciplineLogs) {
                if (dl.getStudent() != null && dl.getStudent().getRegNo() != null) {
                    uniqueStudents.add(dl.getStudent().getRegNo());
                }
                if (dl.getPoints() >= 0) {
                    totalPositive += dl.getPoints();
                } else {
                    totalPenalty += Math.abs(dl.getPoints());
                }

                String category = dl.getSubgroup() != null ? dl.getSubgroup().getName()
                        : (dl.getActivity() != null ? dl.getActivity().getName() : "Discipline");
                String reason = dl.getReason() != null ? dl.getReason() : "Points Update";
                String createdStr = dl.getCreatedAt() != null ? dl.getCreatedAt().toString()
                        : (dl.getIncidentDate() != null ? dl.getIncidentDate().toString() : "");
                String timeKey = createdStr.length() >= 16 ? createdStr.substring(0, 16) : createdStr;

                boolean isGroup = (category != null && category.toUpperCase().contains("GROUP"))
                        || (reason != null && reason.toUpperCase().contains("TEAM"))
                        || (dl.getActivity() != null && "GROUP".equalsIgnoreCase(dl.getActivity().getModeType()));

                if (isGroup) {
                    jjcet.PragatiX.entity.Student st = dl.getStudent();
                    jjcet.PragatiX.entity.Team team = st != null ? st.getTeam() : null;
                    Long teamId = team != null ? team.getId() : 0L;
                    String teamName = team != null && team.getName() != null ? team.getName()
                            : (reason.toUpperCase().startsWith("TEAM") ? reason.split("/")[0].trim() : "Team Activity");
                    String dept = team != null && team.getDepartment() != null ? team.getDepartment().getName()
                            : (st != null && st.getDepartment() != null ? st.getDepartment().getName() : "");
                    String yr = team != null && team.getYear() != null ? team.getYear()
                            : (st != null && st.getYear() != null ? st.getYear() : "");
                    String sec = team != null && team.getSection() != null ? team.getSection().getSectionName()
                            : (st != null && st.getSection() != null ? st.getSection().getSectionName() : "");

                    String groupKey = "DL_" + teamId + "_" + (dl.getActivity() != null ? dl.getActivity().getId() : 0L)
                            + "_" + reason + "_" + timeKey + "_" + dl.getPoints();

                    if (groupDlMap.containsKey(groupKey)) {
                        Map<String, Object> gItem = groupDlMap.get(groupKey);
                        int count = (int) gItem.getOrDefault("memberCount", 1) + 1;
                        gItem.put("memberCount", count);
                        if (st != null) {
                            List<Map<String, Object>> mems = (List<Map<String, Object>>) gItem.get("members");
                            Map<String, Object> mData = new HashMap<>();
                            mData.put("id", st.getId());
                            mData.put("name", st.getFullName());
                            mData.put("regNo", st.getRegNo());
                            mData.put("dept", st.getDepartment() != null ? st.getDepartment().getName() : "");
                            mData.put("gender", st.getGender() != null ? st.getGender() : "");
                            mData.put("points", dl.getPoints());
                            mems.add(mData);
                        }
                    } else {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", dl.getId());
                        item.put("source", "DISCIPLINE_LOG");
                        item.put("isGroup", true);
                        item.put("points", dl.getPoints());
                        item.put("reason", reason);
                        item.put("remarks", dl.getRemarks());
                        item.put("category", category);
                        item.put("createdAt", createdStr);
                        item.put("teamId", teamId);
                        item.put("teamName", teamName);
                        item.put("department", dept);
                        item.put("year", yr);
                        item.put("section", sec);
                        item.put("memberCount", 1);
                        List<Map<String, Object>> mems = new ArrayList<>();
                        if (st != null) {
                            Map<String, Object> mData = new HashMap<>();
                            mData.put("id", st.getId());
                            mData.put("name", st.getFullName());
                            mData.put("regNo", st.getRegNo());
                            mData.put("dept", st.getDepartment() != null ? st.getDepartment().getName() : "");
                            mData.put("gender", st.getGender() != null ? st.getGender() : "");
                            mData.put("points", dl.getPoints());
                            mems.add(mData);
                        }
                        item.put("members", mems);
                        groupDlMap.put(groupKey, item);
                        historyList.add(item);
                    }
                } else {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", dl.getId());
                    item.put("source", "DISCIPLINE_LOG");
                    item.put("isGroup", false);
                    item.put("points", dl.getPoints());
                    item.put("reason", reason);
                    item.put("remarks", dl.getRemarks());
                    item.put("category", category);
                    if (dl.getStudent() != null) {
                        item.put("studentId", dl.getStudent().getId());
                        item.put("studentName", dl.getStudent().getFullName());
                        item.put("studentRegNo", dl.getStudent().getRegNo());
                        item.put("studentDept",
                                dl.getStudent().getDepartment() != null ? dl.getStudent().getDepartment().getName()
                                        : "");
                    }
                    item.put("createdAt", createdStr);
                    historyList.add(item);
                }
            }
        }

        // 2. Process XP Transactions
        if (xpTransactions != null) {
            for (jjcet.PragatiX.entity.XpTransaction tx : xpTransactions) {
                if (tx.getStudent() != null && tx.getStudent().getRegNo() != null) {
                    uniqueStudents.add(tx.getStudent().getRegNo());
                }
                if (tx.getXpPoints() >= 0) {
                    totalPositive += tx.getXpPoints();
                } else {
                    totalPenalty += Math.abs(tx.getXpPoints());
                }

                String category = tx.getCategory() != null ? tx.getCategory() : "Activity";
                String actName = tx.getActivityName() != null ? tx.getActivityName()
                        : (tx.getActivity() != null ? tx.getActivity().getName() : "Activity XP");
                String createdStr = tx.getSubmittedAt() != null ? tx.getSubmittedAt().toString() : "";
                String timeKey = createdStr.length() >= 16 ? createdStr.substring(0, 16) : createdStr;

                boolean isGroup = (category != null && category.toUpperCase().contains("GROUP"))
                        || (actName != null && actName.toUpperCase().contains("TEAM"))
                        || (tx.getActivity() != null && "GROUP".equalsIgnoreCase(tx.getActivity().getModeType()));

                if (isGroup) {
                    jjcet.PragatiX.entity.Student st = tx.getStudent();
                    jjcet.PragatiX.entity.Team team = st != null ? st.getTeam() : null;
                    Long teamId = team != null ? team.getId() : 0L;
                    String teamName = team != null && team.getName() != null ? team.getName()
                            : (actName.toUpperCase().startsWith("TEAM") ? actName.split("/")[0].trim()
                                    : "Team Activity");
                    String dept = team != null && team.getDepartment() != null ? team.getDepartment().getName()
                            : (st != null && st.getDepartment() != null ? st.getDepartment().getName() : "");
                    String yr = team != null && team.getYear() != null ? team.getYear()
                            : (st != null && st.getYear() != null ? st.getYear() : "");
                    String sec = team != null && team.getSection() != null ? team.getSection().getSectionName()
                            : (st != null && st.getSection() != null ? st.getSection().getSectionName() : "");

                    String groupKey = "XP_" + teamId + "_" + (tx.getActivity() != null ? tx.getActivity().getId() : 0L)
                            + "_" + actName + "_" + timeKey + "_" + tx.getXpPoints();

                    if (groupXpMap.containsKey(groupKey)) {
                        Map<String, Object> gItem = groupXpMap.get(groupKey);
                        int count = (int) gItem.getOrDefault("memberCount", 1) + 1;
                        gItem.put("memberCount", count);
                        if (st != null) {
                            List<Map<String, Object>> mems = (List<Map<String, Object>>) gItem.get("members");
                            Map<String, Object> mData = new HashMap<>();
                            mData.put("id", st.getId());
                            mData.put("name", st.getFullName());
                            mData.put("regNo", st.getRegNo());
                            mData.put("dept", st.getDepartment() != null ? st.getDepartment().getName() : "");
                            mData.put("gender", st.getGender() != null ? st.getGender() : "");
                            mData.put("points", tx.getXpPoints());
                            mems.add(mData);
                        }
                    } else {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", tx.getId());
                        item.put("source", "XP_TRANSACTION");
                        item.put("isGroup", true);
                        item.put("points", tx.getXpPoints());
                        item.put("reason", actName);
                        item.put("remarks", tx.getCategory());
                        item.put("category", category);
                        item.put("createdAt", createdStr);
                        item.put("teamId", teamId);
                        item.put("teamName", teamName);
                        item.put("department", dept);
                        item.put("year", yr);
                        item.put("section", sec);
                        item.put("memberCount", 1);
                        List<Map<String, Object>> mems = new ArrayList<>();
                        if (st != null) {
                            Map<String, Object> mData = new HashMap<>();
                            mData.put("id", st.getId());
                            mData.put("name", st.getFullName());
                            mData.put("regNo", st.getRegNo());
                            mData.put("dept", st.getDepartment() != null ? st.getDepartment().getName() : "");
                            mData.put("gender", st.getGender() != null ? st.getGender() : "");
                            mData.put("points", tx.getXpPoints());
                            mems.add(mData);
                        }
                        item.put("members", mems);
                        groupXpMap.put(groupKey, item);
                        historyList.add(item);
                    }
                } else {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", tx.getId());
                    item.put("source", "XP_TRANSACTION");
                    item.put("isGroup", false);
                    item.put("points", tx.getXpPoints());
                    item.put("reason", actName);
                    item.put("remarks", tx.getCategory());
                    item.put("category", category);
                    if (tx.getStudent() != null) {
                        item.put("studentId", tx.getStudent().getId());
                        item.put("studentName", tx.getStudent().getFullName());
                        item.put("studentRegNo", tx.getStudent().getRegNo());
                        item.put("studentDept",
                                tx.getStudent().getDepartment() != null ? tx.getStudent().getDepartment().getName()
                                        : "");
                    }
                    item.put("createdAt", createdStr);
                    historyList.add(item);
                }
            }
        }

        // Sort descending by createdAt
        historyList.sort((a, b) -> {
            String tA = (String) a.get("createdAt");
            String tB = (String) b.get("createdAt");
            if (tA == null && tB == null)
                return 0;
            if (tA == null)
                return 1;
            if (tB == null)
                return -1;
            return tB.compareTo(tA);
        });

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("teacher", adminMapper.toUserResponse(user));
        result.put("totalPositivePoints", totalPositive);
        result.put("totalPenaltyPoints", totalPenalty);
        result.put("totalActions", historyList.size());
        result.put("studentsImpacted", uniqueStudents.size());
        result.put("history", historyList);

        return ResponseEntity.ok(ApiResponse.ok("Teacher points history loaded successfully", result));
    }
}
