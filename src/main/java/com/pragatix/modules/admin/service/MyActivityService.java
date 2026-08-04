package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.User;
import com.pragatix.modules.activity.dto.response.MyActivityResponse;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MyActivityService {

    private static final Logger log = LoggerFactory.getLogger(MyActivityService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final UserRepository userRepository;
    private final AdminAssignmentService adminAssignmentService;

    public MyActivityService(ActivityAssignmentRepository activityAssignmentRepository, UserRepository userRepository,
            AdminAssignmentService adminAssignmentService) {
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.userRepository = userRepository;
        this.adminAssignmentService = adminAssignmentService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<MyActivityResponse>>> getMyActivities() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<MyActivityResponse>>error("User not found"));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findAll();

        List<ActivityAssignment> matchingAssignments = allAssignments.stream()
                .filter(a -> adminAssignmentService.isAssignmentMatching(a, currentUser))
                .collect(Collectors.toList());

        Map<Long, List<ActivityAssignment>> assignmentsByActivity = matchingAssignments.stream()
                .collect(Collectors.groupingBy(a -> a.getActivity().getId()));

        List<MyActivityResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, List<ActivityAssignment>> entry : assignmentsByActivity.entrySet()) {
            List<ActivityAssignment> activityAssignments = entry.getValue();
            ActivityAssignment aa = adminAssignmentService.getPriorityAssignment(activityAssignments);
            Activity act = aa.getActivity();

            List<String> evidenceList = new java.util.ArrayList<>();
            if (act.getEvidence() != null && !act.getEvidence().trim().isEmpty()) {
                for (String ev : act.getEvidence().split(",")) {
                    evidenceList.add(ev.trim());
                }
            }

            responses.add(MyActivityResponse.builder()
                    .activityId(act.getId())
                    .name(act.getName())
                    .description(act.getDescription())
                    .frequency(act.getAwardFrequency())
                    .evidence(evidenceList)
                    .xp(act.getXp())
                    .type(act.getType())
                    .justification(act.getJustification())
                    .departmentId(aa.getDepartment() != null ? aa.getDepartment().getId() : null)
                    .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : "Global")
                    .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
                    .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
                    .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : "")
                    .assignedAt(aa.getAssignedAt())
                    .xpCategory(act.getXpCategory())
                    .awardXp(act.getAwardXp())
                    .awardEnabled(act.getAwardEnabled())
                    .penaltyEnabled(act.getPenaltyEnabled())
                    .penaltyXp(act.getPenaltyXp())
                    .awardType(act.getAwardType())
                    .repeatAllowed(act.isRepeatAllowed())
                    .xpType(act.getXpType())
                    .cap(act.getCap())
                    .awardFrequency(act.getAwardFrequency())
                    .awardDays(act.getAwardDays())
                    .attendanceEngineEnabled(act.getAttendanceEngineEnabled())
                    .attendanceRule(act.getAttendanceRule())
                    .manualEvidenceName(act.getManualEvidenceName())
                    .build());
        }

        responses.sort(java.util.Comparator.comparing(MyActivityResponse::getName));
        log.debug("CC getMyActivities: Activities returned count: {}", responses.size());
        return ResponseEntity.ok(ApiResponse.ok("My activities loaded successfully", responses));
    }
}
