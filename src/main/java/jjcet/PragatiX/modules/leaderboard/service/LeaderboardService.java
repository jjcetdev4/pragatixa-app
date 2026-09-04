package jjcet.PragatiX.modules.leaderboard.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.entity.Year;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.StudentAuthResolver;
import jjcet.PragatiX.modules.leaderboard.dto.response.LeaderboardStudentResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StudentMapper;
import jjcet.PragatiX.repository.YearRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.modules.leaderboard.dto.response.FilterOptionsDto;
import jjcet.PragatiX.entity.Department;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final YearRepository yearRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final StudentMapper studentMapper;
    private final AuthUtils authUtils;
    private final StudentAuthResolver studentAuthResolver;

    public LeaderboardService(StudentRepository studentRepository, UserRepository userRepository,
            YearRepository yearRepository, DepartmentRepository departmentRepository,
            SectionRepository sectionRepository, StudentMapper studentMapper, AuthUtils authUtils,
            StudentAuthResolver studentAuthResolver) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.studentMapper = studentMapper;
        this.authUtils = authUtils;
        this.studentAuthResolver = studentAuthResolver;
    }

    public ApiResponse<List<LeaderboardStudentResponse>> getLeaderboard(Long yearId, Long departmentId, Long sectionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "";
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isAdmin = currentUser != null && authUtils.isAdmin(currentUser);
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equalsIgnoreCase(a.getAuthority()) || "STUDENT".equalsIgnoreCase(a.getAuthority()));

        Long targetDeptId = departmentId;
        Long targetYearId = yearId;
        Long targetSectionId = sectionId;

        // Auto-scope for regular students / captains to strictly compare within their own year
        if (isStudent || (currentUser != null && !isAdmin && !isSuperAdmin)) {
            Student student = null;
            try {
                student = studentAuthResolver.getLoggedInStudent();
            } catch (Exception e) {
                log.debug("Could not resolve student from auth resolver in leaderboard: {}", e.getMessage());
            }
            if (student == null && currentUser != null) {
                student = studentRepository.findByUserId(currentUser.getId())
                        .or(() -> studentRepository.findByRegNo(currentUser.getUsername()))
                        .or(() -> studentRepository.findByEmail(currentUser.getEmail()))
                        .orElse(null);
            }
            if (student != null) {
                Long studentYearId = null;
                if (student.getYearRef() != null) {
                    studentYearId = student.getYearRef().getId();
                } else if (student.getYear() != null) {
                    studentYearId = resolveYearId(student.getYear());
                }
                // SERVER-SIDE ENFORCEMENT: Ignore any client-provided yearId and lock to student's year
                if (studentYearId != null) {
                    targetYearId = studentYearId;
                }
            }
        } else if (isAdmin && !isSuperAdmin) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = resolveYearId(adminYearStr);
                if (adminYearId != null) {
                    targetYearId = adminYearId;
                }
            }
        }

        // Database-scoped query: Do not fetch all students into memory
        List<Student> students;
        if (targetYearId != null && targetDeptId != null && targetSectionId != null) {
            students = studentRepository.findByYearRefIdAndDepartmentIdAndSectionId(targetYearId, targetDeptId, targetSectionId);
        } else if (targetYearId != null && targetDeptId != null) {
            students = studentRepository.findByYearRefIdAndDepartmentId(targetYearId, targetDeptId);
        } else if (targetYearId != null) {
            students = studentRepository.findByYearRefId(targetYearId);
        } else if (targetDeptId != null && targetSectionId != null) {
            students = studentRepository.findByDepartmentIdAndSectionId(targetDeptId, targetSectionId);
        } else if (targetDeptId != null) {
            students = studentRepository.findByDepartmentId(targetDeptId);
        } else {
            students = studentRepository.findByActiveTrue();
        }

        students = students.stream().filter(Student::isActive).collect(Collectors.toList());

        // Secondary filter for section name if needed
        if (targetSectionId != null) {
            Section targetSec = sectionRepository.findById(targetSectionId).orElse(null);
            final String targetSecName = targetSec != null && targetSec.getSectionName() != null
                    ? targetSec.getSectionName().replaceAll("(?i)\\s*-\\s*\\d{4}\\s*Batch.*", "").trim().toLowerCase()
                    : null;

            students = students.stream()
                    .filter(s -> {
                        if (s.getSection() == null) return false;
                        if (s.getSection().getId().equals(targetSectionId)) return true;
                        if (targetSecName != null && s.getSection().getSectionName() != null) {
                            String sSecName = s.getSection().getSectionName().replaceAll("(?i)\\s*-\\s*\\d{4}\\s*Batch.*", "").trim().toLowerCase();
                            return sSecName.equals(targetSecName);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        List<Student> sortedStudents = students.stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getTotalXp(), a.getTotalXp());
                    if (cmp != 0) return cmp;
                    cmp = Integer.compare(b.getScore(), a.getScore());
                    if (cmp != 0) return cmp;
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());

        List<LeaderboardStudentResponse> responses = new ArrayList<>();
        for (int i = 0; i < sortedStudents.size(); i++) {
            responses.add(studentMapper.toLeaderboardResponse(sortedStudents.get(i), i + 1));
        }

        return ApiResponse.ok(responses);
    }

    private Long resolveYearId(String yearStr) {
        if (yearStr == null)
            return null;
        String yTrim = yearStr.trim().toUpperCase();
        Byte yearNo = null;
        if (yTrim.equals("I") || yTrim.equals("1"))
            yearNo = 1;
        else if (yTrim.equals("II") || yTrim.equals("2"))
            yearNo = 2;
        else if (yTrim.equals("III") || yTrim.equals("3"))
            yearNo = 3;
        else if (yTrim.equals("IV") || yTrim.equals("4"))
            yearNo = 4;

        if (yearNo != null) {
            Year y = yearRepository.findByYearNo(yearNo).orElse(null);
            if (y != null)
                return y.getId();
        }
        return null;
    }

    public ApiResponse<FilterOptionsDto> getFilters(Long yearId, Long departmentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "";
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isAdmin = currentUser != null && authUtils.isAdmin(currentUser);
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equalsIgnoreCase(a.getAuthority()) || "STUDENT".equalsIgnoreCase(a.getAuthority()));

        List<FilterOptionsDto.FilterItem> yearFilters = new ArrayList<>();
        List<FilterOptionsDto.FilterItem> deptFilters = new ArrayList<>();
        List<FilterOptionsDto.FilterItem> sectionFilters = new ArrayList<>();

        Long targetYearId = yearId;

        if (isStudent || (currentUser != null && !isAdmin && !isSuperAdmin)) {
            Student student = null;
            try {
                student = studentAuthResolver.getLoggedInStudent();
            } catch (Exception ignored) {}
            if (student == null && currentUser != null) {
                student = studentRepository.findByUserId(currentUser.getId())
                        .or(() -> studentRepository.findByRegNo(currentUser.getUsername()))
                        .or(() -> studentRepository.findByEmail(currentUser.getEmail()))
                        .orElse(null);
            }
            if (student != null) {
                if (student.getYearRef() != null) {
                    targetYearId = student.getYearRef().getId();
                    yearFilters.add(new FilterOptionsDto.FilterItem(student.getYearRef().getId().toString(), student.getYearRef().getYearName()));
                } else if (student.getYear() != null) {
                    targetYearId = resolveYearId(student.getYear());
                    if (targetYearId != null) {
                        yearRepository.findById(targetYearId).ifPresent(y ->
                                yearFilters.add(new FilterOptionsDto.FilterItem(y.getId().toString(), y.getYearName()))
                        );
                    }
                }
            }
        } else if (isAdmin && !isSuperAdmin) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = resolveYearId(adminYearStr);
                if (adminYearId != null) {
                    yearRepository.findById(adminYearId).ifPresent(y -> {
                        yearFilters.add(new FilterOptionsDto.FilterItem(y.getId().toString(), y.getYearName()));
                    });
                }
            }
        } else {
            List<User> yearAdmins = userRepository.findByRoleName("ROLE_ADMIN");
            java.util.Set<Long> assignedIds = new java.util.HashSet<>();
            for (User u : yearAdmins) {
                if (!u.isDeleted() && u.getRoles().stream().noneMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()) || "SUPER_ADMIN".equals(r.getName()))) {
                    if (u.getAssignedYear() != null) {
                        assignedIds.add(u.getAssignedYear().getId());
                    } else if (u.getAcademicYear() != null) {
                        String adminYearStr = AuthUtils.getAssignedYearString(u.getAcademicYear());
                        Long aId = resolveYearId(adminYearStr);
                        if (aId != null) assignedIds.add(aId);
                    }
                }
            }
            if (!assignedIds.isEmpty()) {
                yearRepository.findAll().stream()
                        .filter(y -> assignedIds.contains(y.getId()))
                        .forEach(y -> yearFilters.add(new FilterOptionsDto.FilterItem(y.getId().toString(), y.getYearName())));
            } else {
                yearRepository.findAll()
                        .forEach(y -> yearFilters.add(new FilterOptionsDto.FilterItem(y.getId().toString(), y.getYearName())));
            }
        }

        List<Department> depts = departmentRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .filter(d -> Boolean.TRUE.equals(d.getSupportsSections()))
                .collect(Collectors.toList());

        depts.forEach(d -> {
            String deptCode = (d.getDeptCode() != null && !d.getDeptCode().isBlank())
                    ? d.getDeptCode()
                    : ((d.getCode() != null && !d.getCode().isBlank()) ? d.getCode() : d.getName());
            deptFilters.add(new FilterOptionsDto.FilterItem(d.getId().toString(), deptCode, deptCode));
        });

        if (departmentId != null) {
            List<Section> candidateSecs = new ArrayList<>();
            if (targetYearId != null) {
                final Long finalYId = targetYearId;
                List<Section> studentSecs = studentRepository.findByYearRefIdAndDepartmentId(finalYId, departmentId).stream()
                        .filter(Student::isActive)
                        .map(Student::getSection)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                candidateSecs.addAll(studentSecs);
            } else {
                List<Section> studentSecs = studentRepository.findByDepartmentId(departmentId).stream()
                        .filter(Student::isActive)
                        .map(Student::getSection)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                candidateSecs.addAll(studentSecs);
            }
            try {
                candidateSecs.addAll(sectionRepository.findByDepartment_Id(departmentId));
            } catch (Exception ignored) {}

            java.util.Set<String> seenNames = new java.util.HashSet<>();
            for (Section s : candidateSecs) {
                if (s == null) continue;
                String rawName = s.getSectionName() != null ? s.getSectionName() : "";
                String cleanName = rawName.replaceAll("(?i)\\s*-\\s*\\d{4}\\s*Batch.*", "").trim();
                if (cleanName.equalsIgnoreCase("SECTION")) continue;
                String norm = cleanName.toLowerCase();
                if (!norm.isEmpty() && seenNames.add(norm)) {
                    sectionFilters.add(new FilterOptionsDto.FilterItem(s.getId().toString(), cleanName));
                }
            }
            sectionFilters.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }

        return ApiResponse.ok(new FilterOptionsDto(yearFilters, deptFilters, sectionFilters));
    }
}
