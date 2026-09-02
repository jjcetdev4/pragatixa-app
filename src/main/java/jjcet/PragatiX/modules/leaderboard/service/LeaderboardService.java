package jjcet.PragatiX.modules.leaderboard.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.entity.Year;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StudentMapper;
import jjcet.PragatiX.repository.YearRepository;
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

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final YearRepository yearRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final StudentMapper studentMapper;
    private final AuthUtils authUtils;

    public LeaderboardService(StudentRepository studentRepository, UserRepository userRepository,
            YearRepository yearRepository, DepartmentRepository departmentRepository,
            SectionRepository sectionRepository, StudentMapper studentMapper, AuthUtils authUtils) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.studentMapper = studentMapper;
        this.authUtils = authUtils;
    }

    public ApiResponse<List<StudentResponse>> getLeaderboard(Long yearId, Long departmentId, Long sectionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isAdmin = currentUser != null && authUtils.isAdmin(currentUser);
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        Long targetDeptId = departmentId;
        Long targetYearId = yearId;
        Long targetSectionId = sectionId;

        // Auto-scope for regular students / captains to strictly compare within their own year
        if (currentUser != null && !isAdmin && !isSuperAdmin) {
            Student student = studentRepository.findByUserId(currentUser.getId())
                    .or(() -> studentRepository.findByRegNo(currentUser.getUsername()))
                    .or(() -> studentRepository.findByEmail(currentUser.getEmail()))
                    .orElse(null);
            if (student != null) {
                if (targetYearId == null) {
                    if (student.getYearRef() != null) {
                        targetYearId = student.getYearRef().getId();
                    } else if (student.getYear() != null) {
                        targetYearId = resolveYearId(student.getYear());
                    }
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

        List<Student> students = studentRepository.findAll();
        students = students.stream().filter(Student::isActive).collect(Collectors.toList());

        final Long finalDeptId = targetDeptId;
        if (finalDeptId != null) {
            students = students.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(finalDeptId))
                    .collect(Collectors.toList());
        }

        final Long finalYearId = targetYearId;
        if (finalYearId != null) {
            students = students.stream()
                    .filter(s -> s.getYearRef() != null && s.getYearRef().getId().equals(finalYearId))
                    .collect(Collectors.toList());
        }

        final Long finalSectionId = targetSectionId;
        if (finalSectionId != null) {
            Section targetSec = sectionRepository.findById(finalSectionId).orElse(null);
            final String targetSecName = targetSec != null && targetSec.getSectionName() != null
                    ? targetSec.getSectionName().replaceAll("(?i)\\s*-\\s*\\d{4}\\s*Batch.*", "").trim().toLowerCase()
                    : null;

            students = students.stream()
                    .filter(s -> {
                        if (s.getSection() == null) return false;
                        if (s.getSection().getId().equals(finalSectionId)) return true;
                        if (targetSecName != null && s.getSection().getSectionName() != null) {
                            String sSecName = s.getSection().getSectionName().replaceAll("(?i)\\s*-\\s*\\d{4}\\s*Batch.*", "").trim().toLowerCase();
                            return sSecName.equals(targetSecName);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        List<StudentResponse> responses = students.stream()
                .map(studentMapper::toResponse)
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
            Year year = yearRepository.findByYearNo(yearNo).orElse(null);
            if (year != null)
                return year.getId();
        }
        return null;
    }

    public ApiResponse<FilterOptionsDto> getFilters(Long yearId, Long departmentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isAdmin = currentUser != null && authUtils.isAdmin(currentUser);
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        List<FilterOptionsDto.FilterItem> yearFilters = new ArrayList<>();
        List<FilterOptionsDto.FilterItem> deptFilters = new ArrayList<>();
        List<FilterOptionsDto.FilterItem> sectionFilters = new ArrayList<>();

        if (isAdmin && !isSuperAdmin) {
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

        Long targetYearId = yearId;
        if (currentUser != null && !isAdmin && !isSuperAdmin) {
            Student student = studentRepository.findByUserId(currentUser.getId())
                    .or(() -> studentRepository.findByRegNo(currentUser.getUsername()))
                    .or(() -> studentRepository.findByEmail(currentUser.getEmail()))
                    .orElse(null);
            if (student != null) {
                if (targetYearId == null) {
                    if (student.getYearRef() != null) {
                        targetYearId = student.getYearRef().getId();
                    } else if (student.getYear() != null) {
                        targetYearId = resolveYearId(student.getYear());
                    }
                }
            }
        }

        if (departmentId != null) {
            List<Section> candidateSecs = new ArrayList<>();
            if (targetYearId != null) {
                final Long finalYId = targetYearId;
                List<Section> studentSecs = studentRepository.findAll().stream()
                        .filter(Student::isActive)
                        .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(departmentId))
                        .filter(s -> s.getYearRef() != null && s.getYearRef().getId().equals(finalYId))
                        .map(Student::getSection)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                if (!studentSecs.isEmpty()) {
                    candidateSecs.addAll(studentSecs);
                }
            }
            if (candidateSecs.isEmpty()) {
                candidateSecs.addAll(sectionRepository.findByDepartment_Id(departmentId));
            }

            java.util.Set<String> seenNames = new java.util.HashSet<>();
            for (Section s : candidateSecs) {
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
