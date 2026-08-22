package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.ActivityTemporaryAssignment;
import jjcet.PragatiX.entity.AssignmentScope;
import jjcet.PragatiX.entity.ActivitySubgroup;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.ActivityTemporaryAssignmentRepository;
import jjcet.PragatiX.modules.activity.repository.ActivitySubgroupRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(AdminAssignmentService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityTemporaryAssignmentRepository temporaryAssignmentRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityRepository activityRepository;

    public AdminAssignmentService(ActivityAssignmentRepository activityAssignmentRepository,
            ActivityTemporaryAssignmentRepository temporaryAssignmentRepository,
            ActivitySubgroupRepository activitySubgroupRepository,
            ActivityRepository activityRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.temporaryAssignmentRepository = temporaryAssignmentRepository;
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
                log.warn(
                        "WARNING Invalid dropdown value detected. Dropdown : Subgroup Stored Value : {} Resolved To : Individual Reason : Referenced value no longer exists or is incorrect.",
                        storedSubgroup);

                if (activity.getStage() != null) {
                    ActivitySubgroup individualSubgroup = activitySubgroupRepository
                            .findByStageIdAndNameIgnoreCase(activity.getStage().getId(), "Individual").orElse(null);
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
        LocalDate today = LocalDate.now();

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

            // Check active temporary assignment for today
            Long deptId = aa.getDepartment() != null ? aa.getDepartment().getId() : null;
            Long secId = aa.getSection() != null ? aa.getSection().getId() : null;
            List<ActivityTemporaryAssignment> tempMatches = temporaryAssignmentRepository.findActiveAssignments(
                    activity.getId(), deptId, secId, today);

            if (!tempMatches.isEmpty() && tempMatches.get(0).getTemporaryTeacher() != null) {
                ActivityTemporaryAssignment temp = tempMatches.get(0);
                map.put("teacherId", temp.getTemporaryTeacher().getId());
                map.put("teacherName", temp.getTemporaryTeacher().getFullName() + " (Temporary Today)");
                map.put("teacher", temp.getTemporaryTeacher().getFullName() + " (Temporary Today)");
                map.put("username", temp.getTemporaryTeacher().getUsername());
                map.put("isTemporary", true);
                map.put("assignmentType", "TEMPORARY");
                map.put("originalTeacherId",
                        temp.getOriginalTeacher() != null ? temp.getOriginalTeacher().getId() : null);
                map.put("originalTeacherName",
                        temp.getOriginalTeacher() != null ? temp.getOriginalTeacher().getFullName() : null);
            } else if (aa.getTeacher() != null) {
                map.put("teacherId", aa.getTeacher().getId());
                map.put("teacherName", aa.getTeacher().getFullName());
                map.put("teacher", aa.getTeacher().getFullName());
                map.put("username", aa.getTeacher().getUsername());
                map.put("isTemporary", false);
                map.put("assignmentType", "PERMANENT");
            } else {
                map.put("teacherId", 0);
                map.put("teacherName", "Any Faculty");
                map.put("teacher", "Any Faculty");
                map.put("username", "any");
                map.put("isTemporary", false);
                map.put("assignmentType", "NONE");
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
        if (u.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN")
                        || r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN")
                        || r.getName().equalsIgnoreCase("ROLE_SUPERADMIN"))) {
            return true;
        }

        // Check active temporary assignment for today
        if (a.getActivity() != null) {
            Long deptId = a.getDepartment() != null ? a.getDepartment().getId() : null;
            Long secId = a.getSection() != null ? a.getSection().getId() : null;
            List<ActivityTemporaryAssignment> tempMatches = temporaryAssignmentRepository.findActiveAssignments(
                    a.getActivity().getId(), deptId, secId, LocalDate.now());

            if (!tempMatches.isEmpty()) {
                ActivityTemporaryAssignment temp = tempMatches.get(0);
                if (temp.getTemporaryTeacher() != null && temp.getTemporaryTeacher().getId().equals(u.getId())) {
                    return true;
                }
                if (temp.getOriginalTeacher() != null && temp.getOriginalTeacher().getId().equals(u.getId())) {
                    return false;
                }
                if (a.getTeacher() != null && a.getTeacher().getId().equals(u.getId())) {
                    return false;
                }
            }
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
