package com.pragatix.modules.leaderboard.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Section;
import com.pragatix.entity.Student;
import com.pragatix.entity.User;
import com.pragatix.entity.Year;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.student.service.StudentMapper;
import com.pragatix.repository.YearRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.repository.SectionRepository;
import com.pragatix.modules.leaderboard.dto.response.FilterOptionsDto;
import com.pragatix.entity.Department;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.pragatix.modules.authentication.security.AuthUtils;

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
        boolean isTeacher = currentUser != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_TEACHER"));
        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .anyMatch(sr -> sr.getName().trim().equalsIgnoreCase("CC")
                        || sr.getName().trim().equalsIgnoreCase("CLASS_COORDINATOR"));

        Long targetDeptId = departmentId;
        Long targetYearId = yearId;
        Long targetSectionId = sectionId;

        if (isAdmin && !isSuperAdmin) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = resolveYearId(adminYearStr);
                if (adminYearId != null) {
                    targetYearId = adminYearId;
                }
            }
        } else if (isTeacher && !isAdmin) {
            // Teacher gets scoped down to their assigned class if CC
            if (isCc) {
                targetDeptId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;
                targetSectionId = currentUser.getSection() != null ? currentUser.getSection().getId() : null;
                targetYearId = resolveYearId(currentUser.getYear());
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
            students = students.stream()
                    .filter(s -> s.getSection() != null && s.getSection().getId().equals(finalSectionId))
                    .collect(Collectors.toList());
        }

        List<StudentResponse> responses = students.stream()
                .map(studentMapper::toResponse)
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
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
        boolean isTeacher = currentUser != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_TEACHER"));
        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .anyMatch(sr -> sr.getName().trim().equalsIgnoreCase("CC")
                        || sr.getName().trim().equalsIgnoreCase("CLASS_COORDINATOR"));

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
            List<Department> depts = departmentRepository.findAll();
            depts.forEach(d -> deptFilters.add(new FilterOptionsDto.FilterItem(d.getId().toString(), d.getName())));
            if (departmentId != null) {
                List<Section> secs = sectionRepository.findByDepartment_Id(departmentId);
                secs.forEach(s -> sectionFilters
                        .add(new FilterOptionsDto.FilterItem(s.getId().toString(), s.getSectionName())));
            }
            return ApiResponse.ok(new FilterOptionsDto(yearFilters, deptFilters, sectionFilters));
        } else if (isTeacher && !isAdmin && isCc) {
            // Scoped purely to assigned class, no global filters
            if (currentUser.getYear() != null) {
                Long yId = resolveYearId(currentUser.getYear());
                if (yId != null)
                    yearFilters.add(new FilterOptionsDto.FilterItem(yId.toString(), currentUser.getYear()));
            }
            if (currentUser.getDepartment() != null) {
                deptFilters.add(new FilterOptionsDto.FilterItem(currentUser.getDepartment().getId().toString(),
                        currentUser.getDepartment().getName()));
            }
            if (currentUser.getSection() != null) {
                sectionFilters.add(new FilterOptionsDto.FilterItem(currentUser.getSection().getId().toString(),
                        currentUser.getSection().getSectionName()));
            }
            return ApiResponse.ok(new FilterOptionsDto(yearFilters, deptFilters, sectionFilters));
        }

        // Global/Admin access
        yearRepository.findAll()
                .forEach(y -> yearFilters.add(new FilterOptionsDto.FilterItem(y.getId().toString(), y.getYearName())));

        List<Department> depts = departmentRepository.findAll();
        depts.forEach(d -> deptFilters.add(new FilterOptionsDto.FilterItem(d.getId().toString(), d.getName())));

        if (departmentId != null) {
            List<Section> secs = sectionRepository.findByDepartment_Id(departmentId);
            secs.forEach(
                    s -> sectionFilters.add(new FilterOptionsDto.FilterItem(s.getId().toString(), s.getSectionName())));
        }

        return ApiResponse.ok(new FilterOptionsDto(yearFilters, deptFilters, sectionFilters));
    }
}
