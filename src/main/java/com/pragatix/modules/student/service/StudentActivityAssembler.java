package com.pragatix.modules.student.service;

import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.Student;
import com.pragatix.modules.activity.dto.response.ActivityResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudentActivityAssembler {

    private final StudentAssignmentResolver assignmentResolver;

    public StudentActivityAssembler(StudentAssignmentResolver assignmentResolver) {
        this.assignmentResolver = assignmentResolver;
    }

    public List<ActivityResponse> enrichActivities(
            Student student,
            List<Activity> activities,
            Map<Long, List<ActivityAssignment>> assignmentsByActivity,
            StudentXpAggregator.AggregatedXp aggregatedXp) {

        List<ActivityResponse> enrichedActivities = new ArrayList<>();

        for (Activity act : activities) {
            ActivityResponse actMap = new ActivityResponse();
            actMap.setActivityId(act.getId());

            String currentActivityName = act.getActivityName() != null ? act.getActivityName() : act.getName();
            String normalizedActName = currentActivityName != null
                    ? currentActivityName.trim().toLowerCase().replaceAll("\\s+", " ")
                            .replaceAll("^\\p{Punct}+|\\p{Punct}+$", "")
                    : null;

            actMap.setActivityName(currentActivityName);
            actMap.setDescription(
                    act.getActivityDescription() != null ? act.getActivityDescription() : act.getDescription());
            int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp() : act.getMaxPoints();
            actMap.setRewardXp(rewardXp);

            int sumXp = 0;
            if (aggregatedXp.xpByActivityId.containsKey(act.getId())) {
                sumXp = aggregatedXp.xpByActivityId.get(act.getId());
            } else if (normalizedActName != null && aggregatedXp.xpByActivityName.containsKey(normalizedActName)) {
                sumXp = aggregatedXp.xpByActivityName.get(normalizedActName);
            }

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

            List<ActivityAssignment> assignments = assignmentsByActivity.getOrDefault(act.getId(),
                    java.util.Collections.emptyList());
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
            actMap.setAttendanceEngineEnabled(act.getAttendanceEngineEnabled());
            actMap.setAttendanceRule(act.getAttendanceRule());
            actMap.setManualEvidenceName(act.getManualEvidenceName());

            enrichedActivities.add(actMap);
        }

        return enrichedActivities;
    }
}
