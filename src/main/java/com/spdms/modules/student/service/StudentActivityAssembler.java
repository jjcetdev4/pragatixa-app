package com.spdms.modules.student.service;

import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.ActivityCompletionRequest;
import com.spdms.entity.Student;
import com.spdms.entity.StudentActivityXp;
import com.spdms.modules.activity.dto.response.ActivityResponse;
import com.spdms.repository.ActivityCompletionRequestRepository;
import com.spdms.modules.student.repository.StudentActivityXpRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentActivityAssembler {

    private final StudentAssignmentResolver assignmentResolver;
    private final ActivityCompletionRequestRepository requestRepository;
    private final StudentXpValidator studentXpValidator;
    private final StudentActivityXpRepository studentActivityXpRepository;

    public StudentActivityAssembler(StudentAssignmentResolver assignmentResolver,
                                    ActivityCompletionRequestRepository requestRepository,
                                    StudentXpValidator studentXpValidator,
                                    StudentActivityXpRepository studentActivityXpRepository) {
        this.assignmentResolver = assignmentResolver;
        this.requestRepository = requestRepository;
        this.studentXpValidator = studentXpValidator;
        this.studentActivityXpRepository = studentActivityXpRepository;
    }

    public List<ActivityResponse> enrichActivities(
            Student student,
            List<Activity> activities,
            Map<Long, List<ActivityAssignment>> assignmentsByActivity,
            StudentXpAggregator.AggregatedXp aggregatedXp) {

        List<ActivityResponse> enrichedActivities = new ArrayList<>();
        
        List<ActivityCompletionRequest> studentRequests = requestRepository.findMyRequests(student.getId());
        List<StudentActivityXp> studentXps = studentActivityXpRepository.findByStudentId(student.getId());
        
        Map<Long, Integer> exactXpByActivityId = studentXps.stream()
            .filter(x -> !"FAIL".equals(x.getResult()) && x.getActivity() != null)
            .collect(Collectors.groupingBy(x -> x.getActivity().getId(), Collectors.summingInt(StudentActivityXp::getXpAwarded)));
        
        for (Activity act : activities) {
            ActivityResponse actMap = new ActivityResponse();
            actMap.setActivityId(act.getId());

            String currentActivityName = act.getActivityName() != null ? act.getActivityName() : act.getName();

            actMap.setActivityName(currentActivityName);
            actMap.setDescription(act.getActivityDescription() != null ? act.getActivityDescription() : act.getDescription());
            int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp() : act.getMaxPoints();
            actMap.setRewardXp(rewardXp);

            int sumXp = exactXpByActivityId.getOrDefault(act.getId(), 0);

            Integer cap = act.getCap();
            int awardedXp = sumXp;
            int requiredXp = rewardXp;

            if (cap != null && cap > 1) {
                requiredXp = rewardXp * cap;
            }

            if (awardedXp > requiredXp) {
                awardedXp = requiredXp;
            }

            actMap.setAwardedXp(awardedXp);
            actMap.setRequiredXp(requiredXp);
            actMap.setRemainingXp(Math.max(0, requiredXp - awardedXp));

            actMap.setFrequency(act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
            actMap.setEvidence(act.getEvidence());

            String facultyName = null;
            Long facultyId = null;

            List<ActivityAssignment> assignments = assignmentsByActivity.getOrDefault(act.getId(), java.util.Collections.emptyList());
            ActivityAssignment bestAssignment = assignmentResolver.resolveBestAssignment(student, assignments);

            if (bestAssignment != null && bestAssignment.getTeacher() != null) {
                facultyName = bestAssignment.getTeacher().getFullName();
                facultyId = bestAssignment.getTeacher().getId();
            }

            if (facultyName == null && act.getSubgroup() != null && act.getSubgroup().getAssignedFaculty() != null) {
                facultyName = act.getSubgroup().getAssignedFaculty().getFullName();
                facultyId = act.getSubgroup().getAssignedFaculty().getId();
            }

            actMap.setFacultyName(facultyName);
            actMap.setFacultyId(facultyId);

            boolean completed = awardedXp >= requiredXp;
            String status = completed ? "COMPLETED" : (awardedXp > 0 ? "IN_PROGRESS" : "NOT_STARTED");

            actMap.setCompleted(completed);
            actMap.setStatus(status);
            actMap.setAllowStudentRequest(act.getAllowStudentRequest());
            
            // Populate Button Logic
            actMap.setActivityCompleted(completed);
            
            ActivityCompletionRequest latestReq = studentRequests.stream()
                .filter(r -> r.getActivity().getId().equals(act.getId()))
                .findFirst().orElse(null);
                
            String reqStatus = latestReq != null ? latestReq.getStatus() : null;
            actMap.setRequestStatus(reqStatus);
            
            String limitError = studentXpValidator.checkAwardLimit(student, act);
            boolean isCapReached = limitError != null;
            actMap.setCategoryCapReached(isCapReached);
            
            boolean canReq = Boolean.TRUE.equals(act.getAllowStudentRequest());
            actMap.setCanRequest(canReq);
            
            if (!canReq) {
                actMap.setButtonEnabled(false);
                actMap.setButtonText("Not Requestable");
            } else if (isCapReached && !"REJECTED".equals(reqStatus) && !"PENDING".equals(reqStatus)) {
                actMap.setButtonEnabled(false);
                actMap.setButtonText("Category Cap Reached");
            } else if ("PENDING".equals(reqStatus)) {
                actMap.setButtonEnabled(false);
                actMap.setButtonText("Request Submitted");
            } else if ("REJECTED".equals(reqStatus)) {
                actMap.setButtonEnabled(true);
                actMap.setButtonText("Rejected - Submit Again");
            } else if ("APPROVED".equals(reqStatus)) {
                actMap.setButtonEnabled(false);
                actMap.setButtonText("Already Completed");
            } else if (completed) {
                actMap.setButtonEnabled(false);
                actMap.setButtonText("Already Completed");
            } else {
                actMap.setButtonEnabled(true);
                actMap.setButtonText("Request Completion");
            }
            
            enrichedActivities.add(actMap);
        }

        return enrichedActivities;
    }
}
