package com.pragatix.modules.student.service;

import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.Student;
import com.pragatix.entity.User;
import com.pragatix.modules.student.dto.response.MyActivityStudentsResponse;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StudentXpMapper {

    private final UserRepository userRepository;

    public StudentXpMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public MyActivityStudentsResponse mapToActivityStudentsResponse(Activity activity,
            ActivityAssignment priorityAssignment, List<Student> studentList) {
        List<MyActivityStudentsResponse.StudentDetail> studentDetails = new ArrayList<>();
        for (Student s : studentList) {
            String secName = s.getSection() != null ? s.getSection().getSectionName() : "";
            studentDetails.add(new MyActivityStudentsResponse.StudentDetail(
                    s.getId(),
                    s.getFullName(),
                    s.getRegNo(),
                    s.getDepartment() != null ? s.getDepartment().getName() : "",
                    secName,
                    s.getYear() != null ? s.getYear() : "",
                    s.getTotalXp(),
                    s.getScore()));
        }

        List<String> evidenceList = new ArrayList<>();
        if (activity.getEvidence() != null && !activity.getEvidence().trim().isEmpty()) {
            for (String ev : activity.getEvidence().split(",")) {
                evidenceList.add(ev.trim());
            }
        }

        MyActivityStudentsResponse.ActivityDetail actDetail = new MyActivityStudentsResponse.ActivityDetail(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                activity.getOwnerDepartment(),
                evidenceList,
                activity.getFrequency(),
                activity.getType(),
                activity.getXpCategory(),
                activity.getAwardEnabled(),
                activity.getAwardXp(),
                activity.getPenaltyEnabled(),
                activity.getPenaltyXp(),
                activity.getCap());

        String assignedFacultyName = "Any Faculty";
        String assignmentMode = "Global";
        if (priorityAssignment.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) {
            assignedFacultyName = priorityAssignment.getTeacher() != null
                    ? priorityAssignment.getTeacher().getFullName()
                    : "Any Faculty";
            assignmentMode = "Specific Faculty";
        } else if (priorityAssignment.getAssignmentScope() == AssignmentScope.SECTION
                || priorityAssignment.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
            assignmentMode = "Class Coordinator (Auto Assigned)";
            assignedFacultyName = "Class Coordinator (Auto Assigned)";
            if (priorityAssignment.getDepartment() != null && priorityAssignment.getSection() != null) {
                List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(
                        priorityAssignment.getDepartment().getId(),
                        priorityAssignment.getSection().getId());
                if (!ccs.isEmpty()) {
                    assignedFacultyName = ccs.get(0).getFullName();
                }
            }
        }

        MyActivityStudentsResponse.AssignmentDetail assignDetail = new MyActivityStudentsResponse.AssignmentDetail(
                priorityAssignment.getId(),
                priorityAssignment.getAssignedBy() != null ? priorityAssignment.getAssignedBy().getFullName() : "",
                priorityAssignment.getAssignedAt() != null ? priorityAssignment.getAssignedAt().toString() : "",
                assignedFacultyName,
                assignmentMode);

        int xpLimit = 0;
        try {
            xpLimit = Integer.parseInt(activity.getXp());
        } catch (Exception ignored) {
        }

        return new MyActivityStudentsResponse(actDetail, studentDetails, xpLimit, assignDetail);
    }
}
