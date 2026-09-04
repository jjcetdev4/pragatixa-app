package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.modules.student.dto.response.StudentSelfResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.YearRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
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
    private final jjcet.PragatiX.repository.DepartmentRepository departmentRepository;
    private final jjcet.PragatiX.repository.SectionRepository sectionRepository;
    private final jjcet.PragatiX.repository.TeamRepository teamRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public StudentQueryService(StudentRepository studentRepository, UserRepository userRepository,
            YearRepository yearRepository, StudentMapper studentMapper,
            StudentGuardianRepository studentGuardianRepository, AuthUtils authUtils,
            jjcet.PragatiX.repository.DepartmentRepository departmentRepository,
            jjcet.PragatiX.repository.SectionRepository sectionRepository,
            jjcet.PragatiX.repository.TeamRepository teamRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.studentMapper = studentMapper;
        this.studentGuardianRepository = studentGuardianRepository;
        this.authUtils = authUtils;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.teamRepository = teamRepository;
    }

    public ApiResponse<StudentResponse> getStudentById(Long id) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equalsIgnoreCase(a.getAuthority()) || "STUDENT".equalsIgnoreCase(a.getAuthority()));
        if (isStudent) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: Students are not authorized to view other students by ID.");
        }

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

    public ApiResponse<StudentSelfResponse> getStudentSelfProfile(Student student) {
        if (student == null) {
            return ApiResponse.error("Student profile not found");
        }
        StudentGuardian guardian = studentGuardianRepository.findByStudentId(student.getId()).orElse(null);
        return ApiResponse.ok(studentMapper.toSelfResponse(student, guardian));
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

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy, String keyword,
            String year, Long departmentId, Long sectionId) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = Sort.by(sortBy).ascending();
        if (!"regNo".equalsIgnoreCase(sortBy)) {
            sort = sort.and(Sort.by("regNo").ascending());
        }
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equalsIgnoreCase(a.getAuthority()) || "STUDENT".equalsIgnoreCase(a.getAuthority()));
        if (isStudent) {
            log.warn("Student user attempted to access unauthorized Student Directory API");
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: Students are not authorized to access the student directory.");
        }

        String username = auth != null ? auth.getName() : "";
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
            Long ccSectionId = userSection != null ? userSection.getId() : null;

            if (currentUser.getDepartment() != null && yearRef != null) {
                Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudentsByCC(
                        keyword == null ? "" : keyword,
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        ccSectionId,
                        false,
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            jjcet.PragatiX.entity.Year assignedYear = currentUser.getAssignedYear();
            if (assignedYear != null) {
                Long adminYearId = assignedYear.getId();
                Page<StudentResponse> result = mapWithGuardians(
                        studentRepository.findByFiltersWithYearRef(keyword, adminYearId, departmentId, sectionId, pageable));
                log.info("Admin user '{}' with year id '{}': total students in DB = {}, returned in page = {}",
                        username, adminYearId, result.getTotalElements(), result.getNumberOfElements());
                return ApiResponse.ok(result);
            } else {
                log.warn("Admin user '{}' has no assignedYear; returning 0 students.", username);
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        boolean isHod = currentUser != null && (currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_HOD"))
                || currentUser.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD") || sr.trim().equalsIgnoreCase("HEAD_OF_DEPARTMENT")));
        if (isHod && currentUser != null && !authUtils.isSuperAdmin(currentUser)) {
            Long hodDeptId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;
            if (hodDeptId != null) {
                departmentId = hodDeptId;
            }
        }

        boolean isTeacher = currentUser != null && currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_TEACHER") || r.getName().equalsIgnoreCase("TEACHER"));
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        if (!isSuperAdmin && !isHod && !isTeacher) {
            log.warn("Unauthorized or unknown user '{}' attempted to access student directory", username);
            return ApiResponse.ok(Page.empty(pageable));
        }

        // For Super Admin or other roles, apply all filters
        String yTrim = (year != null && !year.trim().isEmpty() && !year.equalsIgnoreCase("null") && !year.equalsIgnoreCase("all")) ? year.trim() : null;
        String yNo = null;
        Byte yNoByte = null;
        if (yTrim != null) {
            if (yTrim.matches(".*\\d+.*")) {
                yNo = yTrim.replaceAll("[^0-9]", "");
            } else if (yTrim.equalsIgnoreCase("First Year") || yTrim.equalsIgnoreCase("FIRST_YEAR") || yTrim.equalsIgnoreCase("I")) {
                yNo = "1";
            } else if (yTrim.equalsIgnoreCase("Second Year") || yTrim.equalsIgnoreCase("SECOND_YEAR") || yTrim.equalsIgnoreCase("II")) {
                yNo = "2";
            } else if (yTrim.equalsIgnoreCase("Third Year") || yTrim.equalsIgnoreCase("THIRD_YEAR") || yTrim.equalsIgnoreCase("III")) {
                yNo = "3";
            } else if (yTrim.equalsIgnoreCase("Fourth Year") || yTrim.equalsIgnoreCase("FOURTH_YEAR") || yTrim.equalsIgnoreCase("IV")) {
                yNo = "4";
            }
            if (yNo != null) {
                try {
                    yNoByte = Byte.parseByte(yNo);
                } catch (Exception ignored) {}
            }
        }

        Page<StudentResponse> result = mapWithGuardians(
                studentRepository.findByFilters(keyword, yTrim, yNo, yNoByte, departmentId, sectionId, pageable));
        log.info("User '{}': total students in DB = {}, returned in page = {}",
                username, result.getTotalElements(), result.getNumberOfElements());
        return ApiResponse.ok(result);
    }

    public java.util.List<jjcet.PragatiX.entity.Department> getFilterDepartmentsByYear(String year) {
        return departmentRepository.findAll().stream()
                .filter(d -> !d.isDeleted() && (d.getDepartmentType() == null || d.getDepartmentType() == jjcet.PragatiX.enums.DepartmentType.MAIN))
                .filter(d -> !d.getName().toLowerCase().startsWith("department of"))
                .toList();
    }

    public java.util.List<jjcet.PragatiX.entity.Section> getFilterSections(String year, Long departmentId) {
        if (departmentId != null) {
            return sectionRepository.findByDepartment_IdOrderBySectionNameAsc(departmentId);
        }
        return sectionRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(jjcet.PragatiX.entity.Section::getSectionName, java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size, boolean unassignedOnly) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, safeSize,
                Sort.by("fullName").ascending().and(Sort.by("regNo").ascending()));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equalsIgnoreCase(a.getAuthority()) || "STUDENT".equalsIgnoreCase(a.getAuthority()));
        if (isStudent) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: Students are not authorized to search students.");
        }
        String username = auth != null ? auth.getName() : "";
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
            Long ccSectionId = userSection != null ? userSection.getId() : null;

            if (currentUser.getDepartment() != null && yearRef != null) {
                Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudentsByCC(
                        keyword,
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        ccSectionId,
                        unassignedOnly,
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            jjcet.PragatiX.entity.Year assignedYear = currentUser.getAssignedYear();
            if (assignedYear != null) {
                Long adminYearId = assignedYear.getId();
                Page<StudentResponse> result = mapWithGuardians(
                        studentRepository.searchStudentsByYearRef(keyword, adminYearId, unassignedOnly, pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudents(keyword, unassignedOnly, pageable));
        return ApiResponse.ok(result);
    }

    public ApiResponse<java.util.List<jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(
            String keyword, Long teamId, Integer currentStage) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ApiResponse.error("Team not found");
        }

        Long deptId = team.getDepartment() != null ? team.getDepartment().getId() : null;
        Long sectionId = team.getSection() != null ? team.getSection().getId() : null;
        String teamYear = team.getYear();

        java.util.List<Student> rawCandidates;
        if (deptId != null && sectionId != null) {
            rawCandidates = studentRepository.findByDepartmentIdAndSectionId(deptId, sectionId);
        } else if (deptId != null) {
            rawCandidates = studentRepository.findByDepartmentId(deptId);
        } else {
            rawCandidates = studentRepository.findAll();
        }

        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        final int targetStage = (currentStage != null && currentStage > 0) ? currentStage : 1;

        java.util.List<jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO> results = rawCandidates.stream()
                .filter(s -> s != null && s.isActive() && !s.isDeleted())
                .filter(s -> s.getTeam() == null)
                .filter(s -> !s.isCaptain())
                .filter(s -> {
                    if (team.getCaptain() != null && team.getCaptain().getId().equals(s.getId())) {
                        return false;
                    }
                    return true;
                })
                .filter(s -> {
                    if (deptId != null && (s.getDepartment() == null || !s.getDepartment().getId().equals(deptId))) {
                        return false;
                    }
                    if (sectionId != null && (s.getSection() == null || !s.getSection().getId().equals(sectionId))) {
                        return false;
                    }
                    if (teamYear != null && !teamYear.trim().isEmpty() && !isStudentYearMatching(teamYear, s)) {
                        return false;
                    }
                    int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage() : (s.getStage() > 0 ? s.getStage() : 1);
                    if (sStage != targetStage && s.getStage() != targetStage) {
                        return false;
                    }
                    if (!kw.isEmpty()) {
                        String name = s.getFullName() != null ? s.getFullName().toLowerCase() : "";
                        String reg = s.getRegNo() != null ? s.getRegNo().toLowerCase() : "";
                        String spr = s.getSprNo() != null ? s.getSprNo().toLowerCase() : "";
                        if (!name.contains(kw) && !reg.contains(kw) && !spr.contains(kw)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((s1, s2) -> {
                    String n1 = s1.getFullName() != null ? s1.getFullName() : "";
                    String n2 = s2.getFullName() != null ? s2.getFullName() : "";
                    int c = n1.compareToIgnoreCase(n2);
                    if (c != 0) return c;
                    String r1 = s1.getRegNo() != null ? s1.getRegNo() : "";
                    String r2 = s2.getRegNo() != null ? s2.getRegNo() : "";
                    return r1.compareToIgnoreCase(r2);
                })
                .map(s -> {
                    jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO dto = new jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO();
                    dto.setId(s.getId());
                    dto.setFullName(s.getFullName());
                    dto.setRegNo(s.getRegNo());
                    dto.setSprNo(s.getSprNo());
                    dto.setDepartmentName(s.getDepartment() != null ? s.getDepartment().getName() : "N/A");
                    dto.setYear(s.getYearRef() != null ? String.valueOf(s.getYearRef().getYearNo()) : (s.getYear() != null ? s.getYear() : "N/A"));
                    dto.setSection(s.getSection() != null ? s.getSection().getSectionName() : "N/A");
                    dto.setTeamName(s.getTeam() != null ? s.getTeam().getName() : null);
                    dto.setTeamId(s.getTeam() != null ? s.getTeam().getId() : null);
                    int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage() : (s.getStage() > 0 ? s.getStage() : 1);
                    dto.setCurrentStage(sStage);
                    return dto;
                })
                .toList();

        return ApiResponse.ok(results);
    }

    private boolean isStudentYearMatching(String targetYear, Student s) {
        if (targetYear == null || targetYear.trim().isEmpty() || targetYear.equalsIgnoreCase("all")) {
            return true;
        }
        if (s == null) return false;
        if (s.getYear() != null && isYearMatching(targetYear, s.getYear())) {
            return true;
        }
        if (s.getYearRef() != null) {
            if (s.getYearRef().getYearNo() != null && isYearMatching(targetYear, String.valueOf(s.getYearRef().getYearNo()))) {
                return true;
            }
            if (s.getYearRef().getYearName() != null && isYearMatching(targetYear, s.getYearRef().getYearName())) {
                return true;
            }
        }
        if (s.getYear() == null && s.getYearRef() == null) {
            return true;
        }
        return false;
    }

    private boolean isYearMatching(String yr1, String yr2) {
        if (yr1 == null || yr1.trim().isEmpty() || yr1.equalsIgnoreCase("all"))
            return true;
        if (yr2 == null || yr2.trim().isEmpty() || yr2.equalsIgnoreCase("all"))
            return true;
        String y1 = yr1.trim().toLowerCase();
        String y2 = yr2.trim().toLowerCase();
        if (y1.equals(y2))
            return true;

        int n1 = getYearNumber(y1);
        int n2 = getYearNumber(y2);
        if (n1 != -1 && n2 != -1)
            return n1 == n2;
        return false;
    }

    private int getYearNumber(String y) {
        if (y.contains("1") || y.equals("i") || y.contains("first"))
            return 1;
        if (y.contains("2") || y.equals("ii") || y.contains("second"))
            return 2;
        if (y.contains("3") || y.equals("iii") || y.contains("third"))
            return 3;
        if (y.contains("4") || y.equals("iv") || y.contains("fourth"))
            return 4;
        return -1;
    }
}
