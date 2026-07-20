package com.spdms.modules.student.service;

import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.Student;
import com.spdms.repository.ActivityAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentAssignmentResolver {

    private final ActivityAssignmentRepository activityAssignmentRepository;

    public StudentAssignmentResolver(ActivityAssignmentRepository activityAssignmentRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
    }

    public Map<Long, List<ActivityAssignment>> fetchAssignmentsByActivity(List<Long> activityIds) {
        Map<Long, List<ActivityAssignment>> assignmentsByActivity = new HashMap<>();
        if (activityIds.isEmpty()) return assignmentsByActivity;

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityIdIn(activityIds);
        for (ActivityAssignment assignment : allAssignments) {
            if (assignment.getActivity() != null) {
                assignmentsByActivity.computeIfAbsent(assignment.getActivity().getId(), k -> new ArrayList<>()).add(assignment);
            }
        }
        return assignmentsByActivity;
    }

    public ActivityAssignment resolveBestAssignment(Student student, List<ActivityAssignment> assignments) {
        ActivityAssignment bestAssignment = null;
        for (ActivityAssignment assignment : assignments) {
            if (student.getSection() != null && assignment.getSection() != null && assignment.getSection().getId().equals(student.getSection().getId())) {
                bestAssignment = assignment;
                break;
            } else if (student.getSection() != null && student.getSection().getDepartment() != null && assignment.getDepartment() != null && assignment.getDepartment().getId().equals(student.getSection().getDepartment().getId())) {
                bestAssignment = assignment;
            } else if (bestAssignment == null) {
                bestAssignment = assignment;
            }
        }
        return bestAssignment;
    }
}
