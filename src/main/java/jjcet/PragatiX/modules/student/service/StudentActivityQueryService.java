package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.ActivityStage;
import jjcet.PragatiX.entity.AssignmentScope;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.entity.ActivityStageMapping;
import jjcet.PragatiX.modules.activity.repository.ActivityStageMappingRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.modules.activity.service.AssignmentSecurityService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.dto.response.MyActivityStudentsResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
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
    private final ActivityStageRepository activityStageRepository;
    private final ActivityStageMappingRepository activityStageMappingRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final DepartmentRepository departmentRepository;
    private final ActivityRepository activityRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final StudentXpMapper mapper;

    public StudentActivityQueryService(UserRepository userRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            ActivityStageRepository activityStageRepository,
            ActivityStageMappingRepository activityStageMappingRepository,
            StudentRepository studentRepository,
            SectionRepository sectionRepository,
            DepartmentRepository departmentRepository,
            ActivityRepository activityRepository,
            AssignmentSecurityService assignmentSecurityService,
            StudentXpMapper mapper) {
        this.userRepository = userRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityStageRepository = activityStageRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.departmentRepository = departmentRepository;
        this.activityRepository = activityRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.mapper = mapper;
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public ResponseEntity<ApiResponse<List<String>>> getYearsForActivity(Long activityId, String username) {
        return getYearsForActivity(activityId, username, null);
    }

    public ResponseEntity<ApiResponse<List<String>>> getYearsForActivity(Long activityId, String username,
            Long stageId) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<String>>error("User profile not found"));

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<String> years = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                .filter(a -> stageId == null || a.getStage() == null || a.getStage().getId().equals(stageId))
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
        return getDepartmentsForActivity(activityId, year, username, null);
    }

    private boolean isGlobalActivity(Long activityId, Long stageId, List<ActivityAssignment> allAssignments) {
        // 1. Check Activity table
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null && "GLOBAL".equalsIgnoreCase(activity.getAssignmentMode())) {
            return true;
        }

        // 2. Check ActivityStageMapping table
        if (stageId != null) {
            Optional<ActivityStageMapping> stageMapping = activityStageMappingRepository.findByStageIdAndActivityId(stageId, activityId);
            if (stageMapping.isPresent() && "GLOBAL".equalsIgnoreCase(stageMapping.get().getAssignmentMode())) {
                return true;
            }
        }
        List<ActivityStageMapping> mappings = activityStageMappingRepository.findByActivityId(activityId);
        if (mappings.stream().anyMatch(m -> "GLOBAL".equalsIgnoreCase(m.getAssignmentMode()))) {
            return true;
        }

        // 3. Check assignments
        if (allAssignments != null && allAssignments.stream().anyMatch(a ->
                a.getAssignmentScope() == jjcet.PragatiX.entity.AssignmentScope.GLOBAL
                        || "GLOBAL".equalsIgnoreCase(a.getActivity() != null ? a.getActivity().getAssignmentMode() : null))) {
            return true;
        }

        return false;
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentsForActivity(Long activityId,
            String year, String username, Long stageId) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<Map<String, Object>>>error("User profile not found"));

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;
        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);

        // For GLOBAL activities, all teachers can see all departments without assignment filter
        boolean isGlobal = isGlobalActivity(activityId, stageId, allAssignments);

        List<Map<String, Object>> depts;
        if (isGlobal) {
            List<Department> mainDepts = departmentRepository.findByDepartmentTypeAndDeletedFalse(jjcet.PragatiX.enums.DepartmentType.MAIN);
            depts = mainDepts.stream()
                    .filter(d -> !d.getName().toLowerCase().startsWith("department of"))
                    .<Map<String, Object>>map(d -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", d.getId());
                        map.put("name", d.getName());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", depts));
        } else {
            // Find assignments for this user
            List<ActivityAssignment> userAssignments = allAssignments.stream()
                    .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                    .collect(Collectors.toList());

            // Further filter by stage (lenient)
            List<ActivityAssignment> stageFiltered = userAssignments.stream()
                    .filter(a -> {
                        if (stageId == null || a.getStage() == null) return true;
                        if (a.getStage().getId().equals(stageId)) return true;
                        ActivityStage reqStage = activityStageRepository.findById(stageId).orElse(null);
                        return reqStage != null && a.getStage().getDisplayOrder() == reqStage.getDisplayOrder();
                    })
                    .collect(Collectors.toList());

            List<ActivityAssignment> matchingAssignments = stageFiltered.isEmpty() ? userAssignments : stageFiltered;

            // Filter by year if applicable
            List<ActivityAssignment> yearFiltered = matchingAssignments.stream()
                    .filter(a -> a.getYear() == null || isYearMatching(targetYear, a.getYear()))
                    .collect(Collectors.toList());

            List<ActivityAssignment> finalAssignments = yearFiltered.isEmpty() ? matchingAssignments : yearFiltered;

            Set<Department> departmentSet = new LinkedHashSet<>();

            for (ActivityAssignment a : finalAssignments) {
                if (a.getDepartment() != null && !a.getDepartment().isDeleted()) {
                    departmentSet.add(a.getDepartment());
                }
            }

            // Also include the assigned staff's own department from profile if not empty
            if (currentUser.getDepartment() != null && !currentUser.getDepartment().isDeleted()) {
                departmentSet.add(currentUser.getDepartment());
            }

            depts = departmentSet.stream()
                    .filter(d -> d.getDepartmentType() == null || d.getDepartmentType() == jjcet.PragatiX.enums.DepartmentType.MAIN)
                    .filter(d -> !d.getName().toLowerCase().startsWith("department of"))
                    .<Map<String, Object>>map(d -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", d.getId());
                        map.put("name", d.getName());
                        return map;
                    })
                    .collect(Collectors.toList());
        }

        // Fallback: If depts is still empty and currentUser has a department assigned in profile
        if (depts.isEmpty() && currentUser.getDepartment() != null && !currentUser.getDepartment().isDeleted()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", currentUser.getDepartment().getId());
            map.put("name", currentUser.getDepartment().getName());
            depts.add(map);
        }

        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", depts));
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionsForActivity(Long activityId, String year,
            Long departmentId, String username) {
        return getSectionsForActivity(activityId, year, departmentId, username, null);
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionsForActivity(Long activityId, String year,
            Long departmentId, String username, Long stageId) {
        User currentUser = getCurrentUser(username);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<Map<String, Object>>>error("User profile not found"));

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;
        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);

        boolean isGlobal = isGlobalActivity(activityId, stageId, allAssignments);

        if (isGlobal) {
            List<jjcet.PragatiX.entity.Section> allSections = departmentId != null
                    ? sectionRepository.findByDepartment_Id(departmentId)
                    : List.of();

            List<Map<String, Object>> sections = allSections.stream()
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("sectionName", s.getSectionName());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
        }

        List<ActivityAssignment> teacherAssignments = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
                .filter(a -> stageId == null || a.getStage() == null || a.getStage().getId().equals(stageId))
                .filter(a -> isYearMatching(targetYear, a.getYear()))
                .collect(Collectors.toList());

        if (teacherAssignments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                    .<List<Map<String, Object>>>error("Access Denied: You are not assigned to this activity."));
        }

        List<ActivityAssignment> matching = teacherAssignments.stream()
                .filter(a -> departmentId == null || a.getDepartment() == null
                        || a.getDepartment().getId().equals(departmentId)
                        || (currentUser.getDepartment() != null && currentUser.getDepartment().getId().equals(departmentId)))
                .collect(Collectors.toList());

        boolean hasDepartmentLevelOrGlobalAssignment = matching.stream().anyMatch(a -> a.getSection() == null);
        List<jjcet.PragatiX.entity.Section> allSections = departmentId != null
                ? sectionRepository.findByDepartment_Id(departmentId)
                : List.of();

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
        return getStudentsForActivity(activityId, year, departmentId, sectionId, username, null);
    }

    public ResponseEntity<ApiResponse<MyActivityStudentsResponse>> getStudentsForActivity(Long activityId, String year,
            Long departmentId, Long sectionId, String username, Long stageId) {
        User teacher = getCurrentUser(username);
        if (teacher == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<MyActivityStudentsResponse>error("Teacher profile not found"));

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);

        // For GLOBAL activities, bypass the teacher-assignment security check
        boolean isGlobal = isGlobalActivity(activityId, stageId, allAssignments);

        List<ActivityAssignment> matching;
        if (isGlobal) {
            matching = allAssignments.stream()
                    .filter(a -> stageId == null || a.getStage() == null || a.getStage().getId().equals(stageId))
                    .filter(a -> year == null || isYearMatching(year, a.getYear()))
                    .filter(a -> departmentId == null || a.getDepartment() == null
                            || a.getDepartment().getId().equals(departmentId))
                    .filter(a -> sectionId == null || a.getSection() == null
                            || a.getSection().getId().equals(sectionId))
                    .collect(Collectors.toList());
        } else {
            matching = allAssignments.stream()
                    .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
                    .filter(a -> stageId == null || a.getStage() == null || a.getStage().getId().equals(stageId))
                    .filter(a -> year == null || isYearMatching(year, a.getYear()))
                    .filter(a -> departmentId == null || a.getDepartment() == null
                            || a.getDepartment().getId().equals(departmentId)
                            || (teacher.getDepartment() != null && teacher.getDepartment().getId().equals(departmentId)))
                    .filter(a -> sectionId == null || a.getSection() == null
                            || a.getSection().getId().equals(sectionId))
                    .collect(Collectors.toList());
        }

        if (!isGlobal && matching.isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                    .<MyActivityStudentsResponse>error("Access Denied: You are not assigned to this activity."));

        ActivityAssignment priorityAssignment = getPriorityAssignment(matching);
        Activity activity = priorityAssignment != null ? priorityAssignment.getActivity() : null;
        if (activity == null) {
            activity = activityRepository.findById(activityId).orElse(null);
        }
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse
                    .<MyActivityStudentsResponse>error("Activity not found"));
        }

        String targetYear = (year == null || year.trim().isEmpty() || year.equalsIgnoreCase("null") || year.equalsIgnoreCase("all")) ? null : year.trim();

        // Determine the stage order number that this activity belongs to.
        int stageOrder = 0;
        if (stageId != null) {
            ActivityStage targetStage = activityStageRepository.findById(stageId).orElse(null);
            if (targetStage != null) {
                stageOrder = targetStage.getDisplayOrder();
                if (stageOrder <= 0 && targetStage.getStageName() != null) {
                    String num = targetStage.getStageName().replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        try { stageOrder = Integer.parseInt(num); } catch (Exception ignored) {}
                    }
                }
                if (stageOrder <= 0 && targetStage.getName() != null) {
                    String num = targetStage.getName().replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        try { stageOrder = Integer.parseInt(num); } catch (Exception ignored) {}
                    }
                }
            }
        }
        if (stageOrder == 0 && priorityAssignment != null && priorityAssignment.getStage() != null) {
            stageOrder = priorityAssignment.getStage().getDisplayOrder();
        }
        if (stageOrder == 0 && activity != null && activity.getStage() != null) {
            stageOrder = activity.getStage().getDisplayOrder();
        }
        if (stageOrder == 0 && activity != null && activity.getSubgroup() != null
                && activity.getSubgroup().getStage() != null) {
            stageOrder = activity.getSubgroup().getStage().getDisplayOrder();
        }
        final int activityStageOrder = stageOrder > 0 ? stageOrder : 1;

        List<Student> rawStudents;
        if (departmentId != null && sectionId != null) {
            rawStudents = studentRepository.findByDepartmentIdAndSectionId(departmentId, sectionId);
        } else if (departmentId != null) {
            rawStudents = studentRepository.findByDepartmentId(departmentId);
        } else {
            rawStudents = studentRepository.findAll();
        }

        Set<Student> uniqueStudents = new java.util.HashSet<>();
        if (rawStudents != null) {
            for (Student s : rawStudents) {
                if (s == null || !s.isActive() || s.isDeleted()) {
                    continue;
                }
                if (departmentId != null
                        && (s.getDepartment() == null || !s.getDepartment().getId().equals(departmentId))) {
                    continue;
                }
                if (sectionId != null) {
                    if (s.getSection() == null || !s.getSection().getId().equals(sectionId)) {
                        continue;
                    }
                }
                if (targetYear != null && !isStudentYearMatching(targetYear, s)) {
                    continue;
                }
                // Stage filter: only include students currently active in or belonging to this stage
                if (activityStageOrder > 0) {
                    int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage() : (s.getStage() > 0 ? s.getStage() : 1);
                    if (sStage != activityStageOrder && s.getStage() != activityStageOrder) {
                        continue;
                    }
                }
                uniqueStudents.add(s);
            }
        }

        List<Student> studentList = new ArrayList<>(uniqueStudents);

        studentList.sort((s1, s2) -> {
            String n1 = s1.getFullName() != null ? s1.getFullName().trim() : "";
            String n2 = s2.getFullName() != null ? s2.getFullName().trim() : "";
            int comp = n1.compareToIgnoreCase(n2);
            if (comp != 0) {
                return comp;
            }
            String r1 = s1.getRegNo() != null ? s1.getRegNo().trim() : "";
            String r2 = s2.getRegNo() != null ? s2.getRegNo().trim() : "";
            return r1.compareToIgnoreCase(r2);
        });

        MyActivityStudentsResponse response = mapper.mapToActivityStudentsResponse(activity, priorityAssignment,
                studentList);
        return ResponseEntity.ok(ApiResponse.ok("Students retrieved successfully", response));
    }

    public ActivityAssignment getPriorityAssignment(List<ActivityAssignment> matches) {
        if (matches == null || matches.isEmpty())
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

    public boolean isStudentYearMatching(String targetYear, Student s) {
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

    public boolean isYearMatching(String yr1, String yr2) {
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
        if (y == null || y.trim().isEmpty()) return -1;
        String clean = y.trim().toLowerCase();
        if (clean.equals("1") || clean.equals("1st") || clean.equals("i") || clean.contains("first") || clean.equals("year 1") || clean.equals("1st year"))
            return 1;
        if (clean.equals("2") || clean.equals("2nd") || clean.equals("ii") || clean.contains("second") || clean.equals("year 2") || clean.equals("2nd year"))
            return 2;
        if (clean.equals("3") || clean.equals("3rd") || clean.equals("iii") || clean.contains("third") || clean.equals("year 3") || clean.equals("3rd year"))
            return 3;
        if (clean.equals("4") || clean.equals("4th") || clean.equals("iv") || clean.contains("fourth") || clean.equals("year 4") || clean.equals("4th year"))
            return 4;

        if (clean.matches(".*\\b1\\b.*")) return 1;
        if (clean.matches(".*\\b2\\b.*")) return 2;
        if (clean.matches(".*\\b3\\b.*")) return 3;
        if (clean.matches(".*\\b4\\b.*")) return 4;
        return -1;
    }
}
