package com.pragatix.modules.admin.service;

import com.pragatix.entity.User;
import com.pragatix.entity.Activity;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(AdminAssignmentService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityRepository activityRepository;

    public AdminAssignmentService(ActivityAssignmentRepository activityAssignmentRepository,
                                  ActivitySubgroupRepository activitySubgroupRepository,
                                  ActivityRepository activityRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityRepository = activityRepository;
    }

    public void populateActivityTransientFields(Activity activity) {
        populateActivityTransientFields(activity, activity.getStage() != null ? activity.getStage().getId() : null);
    }

    public void populateActivityTransientFields(Activity activity, Long stageId) {
        if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            boolean isValid = true;
            String storedSubgroup = activity.getSubgroup() != null ? activity.getSubgroup().getName() : "null";
            String storedCategory = activity.getSubgroup() != null ? activity.getSubgroup().getCategory() : "null";
            
            if (activity.getSubgroup() == null || !"Individual".equalsIgnoreCase(activity.getSubgroup().getName())) {
                isValid = false;
            }
            
            log.info("================================================");
            log.info("ATTENDANCE ACTIVITY LOAD");
            log.info("Activity ID : {}", activity.getId());
            log.info("Activity Name : {}", activity.getName());
            log.info("Attendance Enabled : {}", activity.getAttendanceEngineEnabled());
            log.info("Stored Subgroup : {}", storedSubgroup);
            log.info("Stored Category : {}", storedCategory);
            log.info("Mode Type : {}", activity.getModeType());
            log.info("Mandatory : {}", activity.isMandatory());
            log.info("Dropdown Status : {}", isValid ? "VALID" : "INVALID");
            log.info("================================================");
            
            if (!isValid) {
                log.warn("WARNING Invalid dropdown value detected. Dropdown : Subgroup Stored Value : {} Resolved To : Individual Reason : Referenced value no longer exists or is incorrect.", storedSubgroup);
                
                if (activity.getStage() != null) {
                    ActivitySubgroup individualSubgroup = activitySubgroupRepository.findByStageIdAndNameIgnoreCase(activity.getStage().getId(), "Individual").orElse(null);
                    if (individualSubgroup != null) {
                        activity.setSubgroup(individualSubgroup);
                        activity.setModeType("Individual");
                        activity.setMandatory(false);
                        activityRepository.save(activity);
                    }
                }
            }
        }

        List<ActivityAssignment> assignments;
        if (stageId != null) {
            assignments = activityAssignmentRepository.findByActivityIdAndStageId(activity.getId(), stageId);
            if (assignments.isEmpty()) {
                assignments = activityAssignmentRepository.findByActivityIdAndStageIdIsNull(activity.getId());
            }
        } else {
            assignments = activityAssignmentRepository.findByActivityId(activity.getId());
        }
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
        if (matches.isEmpty())
            return null;
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SECTION)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT)
                return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL)
                return a;
        }
        return matches.get(0);
    }

}
