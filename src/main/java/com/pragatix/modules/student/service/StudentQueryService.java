package com.pragatix.modules.student.service;

import com.pragatix.dto.*;
import com.pragatix.entity.*;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.YearRepository;
import com.pragatix.repository.StudentGuardianRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentQueryService {
    private static final Logger log = LoggerFactory.getLogger(StudentQueryService.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final YearRepository yearRepository;
    private final StudentMapper studentMapper;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AuthUtils authUtils;

    private final com.pragatix.repository.TeamRepository teamRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public StudentQueryService(StudentRepository studentRepository, UserRepository userRepository,
            YearRepository yearRepository, StudentMapper studentMapper,
            StudentGuardianRepository studentGuardianRepository, AuthUtils authUtils, 
            com.pragatix.repository.TeamRepository teamRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.studentMapper = studentMapper;
        this.studentGuardianRepository = studentGuardianRepository;
        this.authUtils = authUtils;
        this.teamRepository = teamRepository;
    }

    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentRepository.findById(id)
                .map(s -> {
                    User currentUser = authUtils.getCurrentUser();
                    if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
                        String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
                        if (adminYear != null && !adminYear.equals(s.getYear())) {
                            throw new org.springframework.security.access.AccessDeniedException(
                                    "You are not authorized to access this student.");
                        }
                    }
                    StudentGuardian guardian = studentGuardianRepository.findByStudentId(s.getId()).orElse(null);
                    return ApiResponse.ok(studentMapper.toResponse(s, guardian));
                })
                .orElseGet(() -> ApiResponse.error("Student not found with ID: " + id));
    }

    private Page<StudentResponse> mapWithGuardians(Page<Student> page) {
        if (page.isEmpty()) {
            return page.map(studentMapper::toResponse);
        }

        java.util.List<Long> studentIds = page.getContent().stream().map(Student::getId).toList();
        java.util.List<StudentGuardian> guardians = studentGuardianRepository.findByStudentIdIn(studentIds);

        java.util.Map<Long, StudentGuardian> guardianMap = guardians.stream()
                .collect(java.util.stream.Collectors.toMap(
                        g -> g.getStudent().getId(),
                        g -> g,
                        (existing, replacement) -> existing));

        return page.map(s -> studentMapper.toResponse(s, guardianMap.get(s.getId())));
    }

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy, String keyword, String year, Long departmentId, Long sectionId) {
        Sort sort = Sort.by(sortBy).ascending();
        if (!"regNo".equalsIgnoreCase(sortBy)) {
            sort = sort.and(Sort.by("regNo").ascending());
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

        if (isCc && currentUser != null && !authUtils.isSuperAdmin(currentUser)) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1"))
                    yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2"))
                    yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3"))
                    yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4"))
                    yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();

            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                // CC sees only their own department/year/section, but we can allow search keyword
                Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudentsByCC(
                        keyword == null ? "" : keyword,
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        userSection.getId(),
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                // Admin can filter by keyword, department, section, but year is forced to adminYear
                Page<StudentResponse> result = mapWithGuardians(studentRepository.findByFilters(keyword, adminYear, departmentId, sectionId, pageable));
                log.info("Admin user '{}' with year '{}': total students in DB = {}, returned in page = {}",
                        username, adminYear, result.getTotalElements(), result.getNumberOfElements());
                return ApiResponse.ok(result);
            } else {
                log.warn("Admin user '{}' has no academic year assigned; returning 0 students.", username);
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        // For Super Admin or other roles, apply all filters
        Page<StudentResponse> result = mapWithGuardians(studentRepository.findByFilters(keyword, year, departmentId, sectionId, pageable));
        log.info("User '{}': total students in DB = {}, returned in page = {}",
                username, result.getTotalElements(), result.getNumberOfElements());
        return ApiResponse.ok(result);
    }

    public java.util.List<com.pragatix.entity.Department> getFilterDepartmentsByYear(String year) {
        return studentRepository.findDistinctDepartmentsByYear(year);
    }

    public java.util.List<com.pragatix.entity.Section> getFilterSections(String year, Long departmentId) {
        return studentRepository.findDistinctSectionsByYearAndDepartment(year, departmentId);
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending().and(Sort.by("regNo").ascending()));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

        if (isCc && currentUser != null && !authUtils.isSuperAdmin(currentUser)) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1"))
                    yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2"))
                    yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3"))
                    yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4"))
                    yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();

            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudentsByCC(
                        keyword,
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        userSection.getId(),
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                Page<StudentResponse> result = mapWithGuardians(
                        studentRepository.searchStudentsByYear(keyword, adminYear, pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudents(keyword, pageable));
        return ApiResponse.ok(result);
    }

    public ApiResponse<java.util.List<com.pragatix.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(
            String keyword, Long teamId, Integer currentStage) {
        Pageable limit = PageRequest.of(0, 100); // Increased limit for bulk team additions

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ApiResponse.error("Team not found");
        }

        String year = team.getYear();
        Long deptId = team.getDepartment() != null ? team.getDepartment().getId() : null;
        Long sectionId = team.getSection() != null ? team.getSection().getId() : null;

        if (year == null || deptId == null || sectionId == null) {
             return ApiResponse.error("Team configuration is incomplete");
        }

        java.util.List<Student> students = studentRepository.searchEligibleStudentsForTeam(keyword, year, deptId, sectionId, currentStage, limit);

        java.util.List<com.pragatix.modules.student.dto.response.StudentSearchDTO> results = students.stream()
                .map(s -> {
                    com.pragatix.modules.student.dto.response.StudentSearchDTO dto = new com.pragatix.modules.student.dto.response.StudentSearchDTO();
                    dto.setId(s.getId());
                    dto.setFullName(s.getFullName());
                    dto.setRegNo(s.getRegNo());
                    dto.setSprNo(s.getSprNo());
                    dto.setDepartmentName(s.getDepartment() != null ? s.getDepartment().getName() : "N/A");
                    dto.setYear(s.getYearRef() != null ? String.valueOf(s.getYearRef().getYearNo()) : "N/A");
                    dto.setSection(s.getSection() != null ? s.getSection().getSectionName() : "N/A");
                    dto.setTeamName(s.getTeam() != null ? s.getTeam().getName() : null);
                    dto.setTeamId(s.getTeam() != null ? s.getTeam().getId() : null);
                    dto.setCurrentStage(s.getCurrentStage());
                    return dto;
                })
                .toList();

        return ApiResponse.ok(results);
    }
}
