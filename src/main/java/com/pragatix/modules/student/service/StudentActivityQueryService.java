package com.pragatix.modules.student.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.Student;
import com.pragatix.entity.User;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.activity.service.AssignmentSecurityService;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.dto.response.MyActivityStudentsResponse;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentActivityQueryService {

    private final UserRepository userRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final StudentXpMapper mapper;

    public StudentActivityQueryService(UserRepository userRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentRepository studentRepository,
            SectionRepository sectionRepository,
            AssignmentSecurityService assignmentSecurityService,
            StudentXpMapper mapper) {
        this.userRepository = userRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.mapper = mapper;
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public ResponseEntity<ApiResponse<List<String>>> getYearsForActivity(Long activityId, String username) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<String>>error("User profile not found"));

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<String> years = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                .map(ActivityAssignment::getYear)
                .filter(Objects::nonNull)
                .filter(y -> !y.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (years.isEmpty())
            years.add("1");
        return ResponseEntity.ok(ApiResponse.ok("Years retrieved successfully", years));
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentsForActivity(Long activityId,
            String year, String username) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<Map<String, Object>>>error("User profile not found"));

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;
        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<Map<String, Object>> depts = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                .filter(a -> isYearMatching(targetYear, a.getYear()))
                .map(ActivityAssignment::getDepartment)
                .filter(Objects::nonNull)
                .distinct()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getName());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", depts));
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionsForActivity(Long activityId, String year,
            Long departmentId, String username) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<Map<String, Object>>>error("User profile not found"));

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;
        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                .filter(a -> isYearMatching(targetYear, a.getYear()))
                .filter(a -> a.getDepartment() == null || a.getDepartment().getId().equals(departmentId))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                    .<List<Map<String, Object>>>error("Access Denied: You are not assigned to this activity."));
        }

        boolean hasDepartmentLevelOrGlobalAssignment = matching.stream().anyMatch(a -> a.getSection() == null);
        List<com.pragatix.entity.Section> allSections = sectionRepository.findByDepartment_Id(departmentId);

        List<Map<String, Object>> sections = allSections.stream()
                .filter(s -> hasDepartmentLevelOrGlobalAssignment ||
                        matching.stream()
                                .anyMatch(a -> a.getSection() != null && a.getSection().getId().equals(s.getId())))
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("sectionName", s.getSectionName());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
    }

    public ResponseEntity<ApiResponse<MyActivityStudentsResponse>> getStudentsForActivity(Long activityId, String year,
            Long departmentId, Long sectionId, String username) {
        User teacher = getCurrentUser(username);
        if (teacher == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<MyActivityStudentsResponse>error("Teacher profile not found"));

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
                .filter(a -> year == null || isYearMatching(year, a.getYear()))
                .filter(a -> departmentId == null || a.getDepartment() == null
                        || a.getDepartment().getId().equals(departmentId))
                .filter(a -> sectionId == null || a.getSection() == null || a.getSection().getId().equals(sectionId))
                .collect(Collectors.toList());

        if (matching.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                    .<MyActivityStudentsResponse>error("Access Denied: You are not assigned to this activity."));

        ActivityAssignment priorityAssignment = getPriorityAssignment(matching);
        Activity activity = priorityAssignment.getActivity();
        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;

        List<Student> rawStudents;
        if (departmentId != null) {
            if (sectionId != null) {
                rawStudents = studentRepository.findByDepartmentIdAndSectionId(departmentId, sectionId);
            } else {
                rawStudents = studentRepository.findByDepartmentId(departmentId);
            }
        } else {
            rawStudents = studentRepository.findAll();
        }

        Set<Student> uniqueStudents = new java.util.HashSet<>();
        if (rawStudents != null) {
            System.out.println("DEBUG: Total students fetched from DB: " + rawStudents.size());
            for (Student s : rawStudents) {
                if (s.isActive()) {
                    if (departmentId != null
                            && (s.getDepartment() == null || !s.getDepartment().getId().equals(departmentId))) {
                        if (s.getId().equals(75L)) System.out.println("DEBUG: Student 75 excluded by Department mismatch.");
                        continue;
                    }
                    if (!isYearMatching(targetYear, s.getYear())) {
                        if (s.getId().equals(75L)) System.out.println("DEBUG: Student 75 excluded by Year mismatch. DB Year=" + s.getYear() + ", targetYear=" + targetYear);
                        continue;
                    }
                    if (sectionId != null) {
                        if (s.getSection() == null || !s.getSection().getId().equals(sectionId)) {
                            if (s.getId().equals(75L)) System.out.println("DEBUG: Student 75 excluded by Section mismatch. DB Section=" + (s.getSection() != null ? s.getSection().getId() : "null") + ", Expected=" + sectionId);
                            continue;
                        }
                    }
                    uniqueStudents.add(s);
                } else {
                    if (s.getId().equals(75L)) System.out.println("DEBUG: Student 75 excluded because isActive() is false.");
                }
            }
        }

        List<Student> studentList = new ArrayList<>(uniqueStudents);
        System.out.println("DEBUG: Returned Students:");
        for (Student s : studentList) {
            System.out.println(s.getId() + " " + s.getFullName());
        }

        studentList.sort((s1, s2) -> {
            String r1 = s1.getRegNo() != null ? s1.getRegNo() : "";
            String r2 = s2.getRegNo() != null ? s2.getRegNo() : "";
            return r1.compareTo(r2);
        });

        MyActivityStudentsResponse response = mapper.mapToActivityStudentsResponse(activity, priorityAssignment,
                studentList);
        return ResponseEntity.ok(ApiResponse.ok("Students retrieved successfully", response));
    }

    public ActivityAssignment getPriorityAssignment(List<ActivityAssignment> matches) {
        if (matches.isEmpty())
            return null;
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SECTION)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL)
                return a;
        }
        return matches.get(0);
    }

    public boolean isYearMatching(String yr1, String yr2) {
        String y1 = (yr1 == null || yr1.trim().isEmpty()) ? "1" : yr1.trim().toLowerCase();
        String y2 = (yr2 == null || yr2.trim().isEmpty()) ? "1" : yr2.trim().toLowerCase();
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
