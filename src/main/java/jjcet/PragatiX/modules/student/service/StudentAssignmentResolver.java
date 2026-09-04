package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.AssignmentScope;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
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
        if (allValid.isEmpty()) {
            return null;
        }

        allValid.sort((a1, a2) -> {
            boolean t1 = a1.getTeacher() != null;
            boolean t2 = a2.getTeacher() != null;
            if (t1 != t2) {
                return t1 ? -1 : 1;
            }

            int score1 = getScopeScore(a1);
            int score2 = getScopeScore(a2);
            return Integer.compare(score1, score2);
        });

        return allValid.get(0);
    }

    private int getScopeScore(ActivityAssignment a) {
        if (a.getSection() != null || a.getAssignmentScope() == AssignmentScope.SECTION) return 1;
        if (a.getDepartment() != null || a.getAssignmentScope() == AssignmentScope.DEPARTMENT) return 2;
        if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) return 3;
        return 4; // GLOBAL
    }

    public List<ActivityAssignment> resolveAllValidAssignments(Student student, List<ActivityAssignment> assignments) {
        List<ActivityAssignment> validAssignments = new ArrayList<>();
        if (student == null || assignments == null) {
            return validAssignments;
        }

        Long studentDeptId = student.getDepartment() != null ? student.getDepartment().getId()
                : (student.getSection() != null && student.getSection().getDepartment() != null ? student.getSection().getDepartment().getId() : null);
        Long studentSecId = student.getSection() != null ? student.getSection().getId() : null;

        for (ActivityAssignment assignment : assignments) {
            AssignmentScope scope = assignment.getAssignmentScope();
            Long assignDeptId = assignment.getDepartment() != null ? assignment.getDepartment().getId() : null;
            Long assignSecId = assignment.getSection() != null ? assignment.getSection().getId() : null;

            if (scope == AssignmentScope.GLOBAL || (assignDeptId == null && assignSecId == null && scope == null)) {
                if (assignDeptId != null) {
                    if (studentDeptId != null && assignDeptId.equals(studentDeptId)) {
                        validAssignments.add(assignment);
                    }
                } else {
                    validAssignments.add(assignment);
                }
            } else if (scope == AssignmentScope.SECTION || assignSecId != null) {
                if (studentSecId != null && assignSecId != null && assignSecId.equals(studentSecId)) {
                    validAssignments.add(assignment);
                } else if (assignSecId == null && assignDeptId != null && studentDeptId != null && assignDeptId.equals(studentDeptId)) {
                    validAssignments.add(assignment);
                }
            } else if (scope == AssignmentScope.DEPARTMENT || assignDeptId != null) {
                if (studentDeptId != null && assignDeptId != null && assignDeptId.equals(studentDeptId)) {
                    if (assignSecId == null || (studentSecId != null && assignSecId.equals(studentSecId))) {
                        validAssignments.add(assignment);
                    }
                }
            } else if (scope == AssignmentScope.SPECIFIC_FACULTY) {
                if (assignDeptId == null && assignSecId == null) {
                    validAssignments.add(assignment);
                } else if (assignDeptId != null && studentDeptId != null && assignDeptId.equals(studentDeptId)) {
                    if (assignSecId == null || (studentSecId != null && assignSecId.equals(studentSecId))) {
                        validAssignments.add(assignment);
                    }
                }
            }
        }
        return validAssignments;
    }
}

