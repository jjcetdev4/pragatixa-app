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
        boolean isAdmin = u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isSuperAdmin = u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));

        if (isAdmin && !isSuperAdmin && u.getAssignedAcademicYear() != null) {
            String adminYear = u.getAssignedAcademicYear().name();
            // If assignment has a year and it doesn't match, it's not for this admin
            if (a.getYear() != null && !a.getYear().equalsIgnoreCase(adminYear)) {
                return false;
            }
            return true;
        }

        if (isAdmin || isSuperAdmin) {
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

    public List<Activity> filterActivitiesForUser(List<Activity> activities, User u) {
        boolean isAdmin = u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isSuperAdmin = u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));

        if (!isAdmin || isSuperAdmin) {
            return activities;
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findAll();
        Map<Long, List<ActivityAssignment>> assignmentsByActivity = allAssignments.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getActivity().getId()));

        return activities.stream().filter(activity -> {
            List<ActivityAssignment> activityAssignments = assignmentsByActivity.getOrDefault(activity.getId(), new ArrayList<>());
            if (activityAssignments.isEmpty()) {
                // If no assignments exist, assume it's global and allow it?
                // Or maybe year admins shouldn't see unassigned activities? Let's say yes for now, or match it against their year.
                // Wait, if it's completely unassigned, it has no Year. Year Admin sees it if they see Global.
                return true; 
            }
            return activityAssignments.stream().anyMatch(a -> isAssignmentMatching(a, u));
        }).collect(java.util.stream.Collectors.toList());
    }

}
