package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.dto.request.CreatePenaltyRequestDto;
import jjcet.PragatiX.modules.student.dto.response.PenaltyActivityDto;
import jjcet.PragatiX.modules.student.dto.response.PenaltyRequestDto;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.PenaltyRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PenaltyWorkflowService {

    private final PenaltyRequestRepository penaltyRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final XpEngineService xpEngineService;
    private final ActivityAssignmentRepository activityAssignmentRepository;

    public PenaltyWorkflowService(PenaltyRequestRepository penaltyRequestRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            ActivityRepository activityRepository,
            XpEngineService xpEngineService,
            ActivityAssignmentRepository activityAssignmentRepository) {
        this.penaltyRequestRepository = penaltyRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.xpEngineService = xpEngineService;
        this.activityAssignmentRepository = activityAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getPendingCount(String username) {
        Optional<User> ccOpt = userRepository.findByUsername(username);
        if (ccOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User cc = ccOpt.get();
        Long deptId = cc.getDepartment() != null ? cc.getDepartment().getId() : null;
        Long sectionId = cc.getSection() != null ? cc.getSection().getId() : null;

        long count = penaltyRequestRepository.countPendingForCc(cc.getId(), deptId, sectionId);

        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", count);
        return ApiResponse.ok("Pending penalty request count", data);
    }

    private boolean isUserCcForStudent(User teacher, Student student) {
        boolean hasCcRole = teacher.getSubRoles().stream().anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName()));
        boolean sameSection = teacher.getSection() != null && student.getSection() != null
                && teacher.getSection().getId().equals(student.getSection().getId());
        boolean sameDepartment = teacher.getDepartment() != null && student.getDepartment() != null
                && teacher.getDepartment().getId().equals(student.getDepartment().getId());
        return hasCcRole && sameSection && sameDepartment;
    }

    private User findCcForStudent(Student student) {
        List<User> users = userRepository.findAll(); // Optimization: could write a specific query
        return users.stream()
                .filter(u -> u.getSubRoles().stream().anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName())))
                .filter(u -> u.getSection() != null && student.getSection() != null
                        && u.getSection().getId().equals(student.getSection().getId()))
                .filter(u -> u.getDepartment() != null && student.getDepartment() != null
                        && u.getDepartment().getId().equals(student.getDepartment().getId()))
                .filter(User::isActive)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public ApiResponse<PenaltyRequestDto> submitPenalty(CreatePenaltyRequestDto dto, String username) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(dto.getRegNo());
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        Optional<User> teacherOpt = userRepository.findByUsername(username);
        if (teacherOpt.isEmpty()) {
            return ApiResponse.error("Teacher not found");
        }
        User teacher = teacherOpt.get();

        Activity activity = null;
        if (dto.getActivityId() != null) {
            activity = activityRepository.findById(dto.getActivityId()).orElse(null);
        }

        int configuredXp = 0;
        if (activity != null) {
            Integer px = activity.getPenaltyEnabled() != null && activity.getPenaltyEnabled() ? activity.getPenaltyXp()
                    : activity.getAwardXp();
            if (px == null) {
                px = activity.getAwardXp();
            }
            if (px != null) {
                configuredXp = Math.abs(px);
            }
        } else {
            configuredXp = Math.abs(dto.getPenaltyXP());
        }

        PenaltyRequest request = new PenaltyRequest();
        request.setStudent(student);
        request.setTeacher(teacher);
        request.setTeacherName(teacher.getFullName());
        request.setActivity(activity);
        request.setActivityName(dto.getActivityName() != null ? dto.getActivityName()
                : (activity != null ? activity.getActivityName() : "Custom Penalty"));
        request.setPenaltyXP(configuredXp);
        request.setReason(dto.getReason());

        boolean isCc = isUserCcForStudent(teacher, student);

        if (isCc) {
            request.setStatus("AUTO_APPROVED");
            request.setApprovedAt(LocalDateTime.now());
            request.setApprovedBy(teacher.getFullName());
            // Immediately apply penalty
            xpEngineService.awardXp(student, activity, null, null, -request.getPenaltyXP(),
                    "Penalty: " + request.getActivityName() + " - " + request.getReason());
        } else {
            request.setStatus("PENDING");
            User cc = findCcForStudent(student);
            if (cc != null) {
                request.setCc(cc);
                request.setCcName(cc.getFullName());
            }
        }

        PenaltyRequest saved = penaltyRequestRepository.save(request);

        System.out.println("Penalty Request Saved");
        System.out.println("Request ID: " + saved.getId());
        System.out.println("Teacher ID: " + (saved.getTeacher() != null ? saved.getTeacher().getId() : "null"));
        System.out.println("Student ID: " + (saved.getStudent() != null ? saved.getStudent().getId() : "null"));
        System.out.println("CC ID: " + (saved.getCc() != null ? saved.getCc().getId() : "null"));
        System.out.println("Status: " + saved.getStatus());

        return ApiResponse.ok("Penalty submitted successfully", mapToDto(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PenaltyRequestDto>> getCcInbox(String username, String status) {
        Optional<User> ccOpt = userRepository.findByUsername(username);
        if (ccOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User cc = ccOpt.get();
        List<PenaltyRequest> requests = penaltyRequestRepository.findByCcIdAndOptionalStatus(cc.getId(), status);

        System.out.println("CC Inbox");
        System.out.println("Logged-in CC: " + username);
        System.out.println("CC ID: " + cc.getId());
        System.out.println("Rows Returned: " + requests.size());
        System.out.println("Request IDs: " + requests.stream().map(PenaltyRequest::getId).collect(Collectors.toList()));

        List<PenaltyRequestDto> dtos = requests.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.ok("Fetched CC inbox", dtos);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PenaltyRequestDto>> getMyRequests(String username) {
        Optional<User> teacherOpt = userRepository.findByUsername(username);
        if (teacherOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User teacher = teacherOpt.get();
        List<PenaltyRequest> requests = penaltyRequestRepository.findByTeacherId(teacher.getId());

        System.out.println("My Requests");
        System.out.println("Logged-in Teacher: " + username);
        System.out.println("Teacher ID: " + teacher.getId());
        System.out.println("Rows Returned: " + requests.size());
        System.out.println("Request IDs: " + requests.stream().map(PenaltyRequest::getId).collect(Collectors.toList()));

        List<PenaltyRequestDto> dtos = requests.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.ok("Fetched my requests", dtos);
    }

    @Transactional
    public ApiResponse<PenaltyRequestDto> approvePenalty(Long id, String username) {
        Optional<PenaltyRequest> reqOpt = penaltyRequestRepository.findById(id);
        if (reqOpt.isEmpty()) {
            return ApiResponse.error("Penalty request not found");
        }
        PenaltyRequest request = reqOpt.get();

        if (!"PENDING".equals(request.getStatus())) {
            return ApiResponse.error("Request is not in PENDING state");
        }

        Optional<User> ccOpt = userRepository.findByUsername(username);
        if (ccOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User cc = ccOpt.get();

        if (request.getCc() == null || !request.getCc().getId().equals(cc.getId())) {
            return ApiResponse.error("Unauthorized: You are not the assigned CC for this request");
        }

        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(cc.getFullName());

        int configuredXp = request.getPenaltyXP();
        if (request.getActivity() != null) {
            Integer px = request.getActivity().getPenaltyEnabled() != null && request.getActivity().getPenaltyEnabled()
                    ? request.getActivity().getPenaltyXp()
                    : request.getActivity().getAwardXp();
            if (px == null)
                px = request.getActivity().getAwardXp();
            if (px != null)
                configuredXp = Math.abs(px);
        }

        // Apply penalty
        xpEngineService.awardXp(request.getStudent(), request.getActivity(), null, null, -configuredXp,
                "Penalty: " + request.getActivityName() + " - " + request.getReason());

        PenaltyRequest saved = penaltyRequestRepository.save(request);
        return ApiResponse.ok("Penalty approved", mapToDto(saved));
    }

    @Transactional
    public ApiResponse<PenaltyRequestDto> rejectPenalty(Long id, String username, String reason) {
        Optional<PenaltyRequest> reqOpt = penaltyRequestRepository.findById(id);
        if (reqOpt.isEmpty()) {
            return ApiResponse.error("Penalty request not found");
        }
        PenaltyRequest request = reqOpt.get();

        if (!"PENDING".equals(request.getStatus())) {
            return ApiResponse.error("Request is not in PENDING state");
        }

        Optional<User> ccOpt = userRepository.findByUsername(username);
        if (ccOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User cc = ccOpt.get();

        if (request.getCc() == null || !request.getCc().getId().equals(cc.getId())) {
            return ApiResponse.error("Unauthorized: You are not the assigned CC for this request");
        }

        request.setStatus("REJECTED");
        request.setRejectedReason(reason);
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(cc.getFullName());

        PenaltyRequest saved = penaltyRequestRepository.save(request);
        return ApiResponse.ok("Penalty rejected", mapToDto(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PenaltyActivityDto>> getGlobalPenaltyActivities() {
        List<Activity> allActivities = activityRepository.findAll();
        List<PenaltyActivityDto> result = allActivities.stream()
                .filter(a -> Boolean.TRUE.equals(a.getPenaltyEnabled()))
                .map(a -> {
                    PenaltyActivityDto dto = new PenaltyActivityDto();
                    dto.setId(a.getId());
                    dto.setName(a.getActivityName());
                    dto.setDescription(a.getDescription());
                    dto.setPenaltyXp(a.getPenaltyXp());
                    dto.setPenaltyEnabled(a.getPenaltyEnabled());
                    return dto;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok("Global penalty activities", result);
    }

    private PenaltyRequestDto mapToDto(PenaltyRequest p) {
        PenaltyRequestDto dto = new PenaltyRequestDto();
        dto.setId(p.getId());
        if (p.getStudent() != null) {
            dto.setStudentName(p.getStudent().getFullName());
            dto.setRegNo(p.getStudent().getRegNo());
            if (p.getStudent().getDepartment() != null)
                dto.setDepartment(p.getStudent().getDepartment().getName());
            if (p.getStudent().getYear() != null)
                dto.setYear(p.getStudent().getYear());
            if (p.getStudent().getSection() != null)
                dto.setSection(p.getStudent().getSection().getSectionName());
        }
        dto.setPenaltyActivity(p.getActivityName());
        dto.setPenaltyXP(p.getPenaltyXP());
        dto.setReason(p.getReason());
        dto.setSubmittedBy(p.getTeacherName());
        dto.setSubmittedTime(p.getCreatedAt());
        dto.setStatus(p.getStatus());
        dto.setApprovedBy(p.getApprovedBy());
        dto.setApprovalTime(p.getApprovedAt());
        dto.setRejectedReason(p.getRejectedReason());
        return dto;
    }

    /**
     * Resolve the execution stage order for student eligibility validation.
     * Priority: assignment.getStage() > activity.getStage() > subgroup stage > 0
     * (no restriction)
     */
    private int resolveExecutionStageOrder(ActivityAssignment assignment, Activity activity) {
        if (assignment != null && assignment.getStage() != null) {
            return assignment.getStage().getDisplayOrder();
        }
        if (activity != null && activity.getStage() != null) {
            return activity.getStage().getDisplayOrder();
        }
        if (activity != null && activity.getSubgroup() != null && activity.getSubgroup().getStage() != null) {
            return activity.getSubgroup().getStage().getDisplayOrder();
        }
        return 0;
    }
}
