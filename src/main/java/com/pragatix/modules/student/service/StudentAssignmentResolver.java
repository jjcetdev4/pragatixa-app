package com.pragatix.modules.student.service;

import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.Student;
import com.pragatix.repository.ActivityAssignmentRepository;
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
        if (activityIds.isEmpty())
            return assignmentsByActivity;

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityIdIn(activityIds);
        for (ActivityAssignment assignment : allAssignments) {
            if (assignment.getActivity() != null) {
                assignmentsByActivity.computeIfAbsent(assignment.getActivity().getId(), k -> new ArrayList<>())
                        .add(assignment);
            }
        }
        return assignmentsByActivity;
    }

    public ActivityAssignment resolveBestAssignment(Student student, List<ActivityAssignment> assignments) {
        List<ActivityAssignment> allValid = resolveAllValidAssignments(student, assignments);
        return allValid.isEmpty() ? null : allValid.get(0);
    }

    public List<ActivityAssignment> resolveAllValidAssignments(Student student, List<ActivityAssignment> assignments) {
        List<ActivityAssignment> validAssignments = new ArrayList<>();

        for (ActivityAssignment assignment : assignments) {
            if (student.getSection() != null && assignment.getSection() != null
                    && assignment.getSection().getId().equals(student.getSection().getId())) {
                validAssignments.add(assignment);
            } else if (student.getSection() != null && student.getSection().getDepartment() != null
                    && assignment.getDepartment() != null
                    && assignment.getDepartment().getId().equals(student.getSection().getDepartment().getId())) {
                validAssignments.add(assignment);
            } else if (assignment.getSection() == null && assignment.getDepartment() == null) {
                validAssignments.add(assignment);
            }
        }
        return validAssignments;
    }
}
