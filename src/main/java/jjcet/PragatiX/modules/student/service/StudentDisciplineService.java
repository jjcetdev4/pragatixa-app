package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.activity.dto.request.*;
import jjcet.PragatiX.modules.activity.dto.response.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.faculty.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StudentDisciplineService {
    private static final Logger log = LoggerFactory.getLogger(StudentDisciplineService.class);

    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentMapper studentMapper;
    private final XpEngineService xpEngineService;
    private final XpTransactionRepository xpTransactionRepository;

    public StudentDisciplineService(ActivitySubgroupRepository activitySubgroupRepository,
            DisciplineLogRepository disciplineLogRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            StudentMapper studentMapper,
            XpEngineService xpEngineService,
            XpTransactionRepository xpTransactionRepository) {
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.studentMapper = studentMapper;
        this.xpEngineService = xpEngineService;
        this.xpTransactionRepository = xpTransactionRepository;
    }

    @Transactional
    public ApiResponse<StudentResponse> adjustPoints(Long regNo, PointAdjustmentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        Student student = studentRepository.findById(regNo).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found");
        }

        ActivitySubgroup subgroup = null;
        if (request.getSubgroupId() != null) {
            subgroup = activitySubgroupRepository.findById(request.getSubgroupId()).orElse(null);
            if (subgroup == null) {
                return ApiResponse.error("Activity subgroup not found");
            }

            // Verify assignment:
            if (subgroup.getAssignedFaculty() != null) {
                // If it is assigned to a specific faculty, verify that the logged-in user
                // matches the assignee
                if (!subgroup.getAssignedFaculty().getId().equals(creator.getId())) {
                    boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        return ApiResponse.error("Access Denied: Only the assigned faculty ("
                                + subgroup.getAssignedFaculty().getFullName()
                                + ") can award points for this activity.");
                    }
                }
            }
        }

        // Adjust point score using centralized XP Engine
        Student saved = xpEngineService.awardXp(student, null, creator, null, request.getPoints(), request.getReason());

        // Record log
        DisciplineLog logEntry = DisciplineLog.builder()
                .student(saved)
                .points(request.getPoints())
                .reason(request.getReason())
                .subgroup(subgroup)
                .recordedBy(creator)
                .incidentDate(LocalDateTime.now())
                .build();
        disciplineLogRepository.save(logEntry);

        log.debug("Teacher {} adjusted student {} points by {}. Reason: {}", creator.getUsername(), saved.getRegNo(),
                request.getPoints(), request.getReason());
        return ApiResponse.ok("Points updated successfully", studentMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<?>> getDisciplineLogs(Long regNo) {
        Student student = studentRepository.findById(regNo).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found");
        }

        List<Map<String, Object>> combined = new java.util.ArrayList<>();

        // 1. Fetch Discipline Logs (manual point adjustments)
        List<DisciplineLog> logs = disciplineLogRepository.findByStudentIdOrderByCreatedAtDesc(regNo);
        if (logs != null) {
            for (DisciplineLog dl : logs) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", dl.getId());
                item.put("points", dl.getPoints());
                item.put("reason", dl.getReason());
                item.put("remarks", dl.getRemarks());
                item.put("recordedByName", dl.getRecordedBy() != null ? dl.getRecordedBy().getFullName() : "Faculty");
                item.put("subgroupName", dl.getSubgroup() != null ? dl.getSubgroup().getName()
                        : (dl.getActivity() != null ? dl.getActivity().getName() : "Discipline"));
                item.put("createdAt", dl.getCreatedAt() != null ? dl.getCreatedAt().toString()
                        : (dl.getIncidentDate() != null ? dl.getIncidentDate().toString() : null));
                combined.add(item);
            }
        }

        // 2. Fetch XP Transactions (Attendance, Activities, Penalties, Captaincy)
        List<XpTransaction> txs = xpTransactionRepository.findByStudentRegNo(student.getRegNo());
        if (txs != null) {
            for (XpTransaction tx : txs) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", tx.getId());
                item.put("points", tx.getXpPoints());
                item.put("reason", tx.getActivityName() != null && !tx.getActivityName().isEmpty()
                        ? tx.getActivityName()
                        : (tx.getActivity() != null ? tx.getActivity().getName() : "XP Activity"));
                item.put("remarks", tx.getCategory());
                item.put("recordedByName", tx.getApprovedBy() != null && !tx.getApprovedBy().isEmpty()
                        ? tx.getApprovedBy()
                        : "System");
                item.put("subgroupName", tx.getCategory() != null ? tx.getCategory() : "Activity");
                item.put("createdAt", tx.getSubmittedAt() != null ? tx.getSubmittedAt().toString() : null);
                combined.add(item);
            }
        }

        // 3. Sort descending by createdAt
        combined.sort((a, b) -> {
            String tA = (String) a.get("createdAt");
            String tB = (String) b.get("createdAt");
            if (tA == null && tB == null) return 0;
            if (tA == null) return 1;
            if (tB == null) return -1;
            return tB.compareTo(tA);
        });

        return ApiResponse.ok("Discipline logs loaded", combined);
    }

    @Transactional(readOnly = true)
    public ApiResponse<DepartmentPerformanceResponse> getDepartmentPerformance(String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        boolean isHodOrAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName)
                        .anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));

        if (!isHodOrAdmin) {
            return ApiResponse.error("Access Denied: Only Head of Department (HOD) can see department performance.");
        }

        Department department = creator.getDepartment();
        if (department == null) {
            return ApiResponse.error("No department assigned to this user.");
        }

        List<Student> students = studentRepository.findByDepartmentId(department.getId());
        long totalStudents = students.size();
        double overallAverage = students.stream()
                .mapToDouble(Student::getScore)
                .average()
                .orElse(100.0);

        Map<String, Double> yearWiseAverage = students.stream()
                .filter(s -> s.getAcademicYear() != null && !s.getAcademicYear().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        Student::getAcademicYear,
                        TreeMap::new,
                        Collectors.averagingDouble(Student::getScore)));

        DepartmentPerformanceResponse response = new DepartmentPerformanceResponse(
                department.getName(),
                overallAverage,
                totalStudents,
                yearWiseAverage);

        return ApiResponse.ok("Department performance metrics loaded", response);
    }

}
