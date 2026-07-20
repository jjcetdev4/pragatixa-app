package com.spdms.modules.admin.service;

import com.spdms.entity.User;
import com.spdms.entity.Activity;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.AssignmentScope;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@Service
public class AdminAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(AdminAssignmentService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;

    public AdminAssignmentService(ActivityAssignmentRepository activityAssignmentRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
    }

    public void populateActivityTransientFields(Activity activity) {
        List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<Map<String, Object>> summary = new ArrayList<>();
        
        for (ActivityAssignment aa : assignments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", aa.getId());
            map.put("scope", aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : "");
            map.put("departmentId", aa.getDepartment() != null ? aa.getDepartment().getId() : null);
            map.put("departmentName", aa.getDepartment() != null ? aa.getDepartment().getName() : "Global");
            map.put("sectionId", aa.getSection() != null ? aa.getSection().getId() : null);
            map.put("section", aa.getSection() != null ? aa.getSection().getSectionName() : null);
            map.put("sectionName", aa.getSection() != null ? aa.getSection().getSectionName() : null);
            map.put("assignmentMode", activity.getAssignmentMode());
            
            if (aa.getTeacher() != null) {
                map.put("teacherId", aa.getTeacher().getId());
                map.put("teacherName", aa.getTeacher().getFullName());
                map.put("teacher", aa.getTeacher().getFullName());
                map.put("username", aa.getTeacher().getUsername());
            } else {
                map.put("teacherId", 0);
                map.put("teacherName", "Any Faculty");
                map.put("teacher", "Any Faculty");
                map.put("username", "any");
            }
            summary.add(map);
        }
        activity.setAssignmentSummary(summary);
        
        // Populate departmentId for backward compat if there's any department set
        if (!assignments.isEmpty() && assignments.get(0).getDepartment() != null) {
            activity.setDepartmentId(assignments.get(0).getDepartment().getId().toString());
        }
    }

    public boolean isAssignmentMatching(ActivityAssignment a, User u) {
        if (u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))) {
            return true;
        }
        
        // GLOBAL scope
        if (a.getAssignmentScope() == AssignmentScope.GLOBAL) {
            return true;
        }

        return a.getTeacher() != null && a.getTeacher().getId().equals(u.getId());
    }

    public ActivityAssignment getPriorityAssignment(List<ActivityAssignment> matches) {
        if (matches.isEmpty()) return null;
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SECTION) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL) return a;
        }
        return matches.get(0);
    }

}
