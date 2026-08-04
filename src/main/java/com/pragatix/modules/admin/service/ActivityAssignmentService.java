package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.Department;
import com.pragatix.entity.Section;
import com.pragatix.entity.User;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.repository.SectionRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.activity.dto.request.AssignmentRequest;
import com.pragatix.modules.activity.dto.response.ActivityAssignmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pragatix.modules.student.repository.StudentActivityXpRepository;

import com.pragatix.entity.ActivityStage;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.entity.ActivityStageMapping;
import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;

@Service
public class ActivityAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ActivityAssignmentService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final ActivityStageRepository activityStageRepository;
    private final ActivityStageMappingRepository activityStageMappingRepository;

    public ActivityAssignmentService(ActivityAssignmentRepository activityAssignmentRepository,
            ActivityRepository activityRepository,
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            ActivityStageRepository activityStageRepository,
            ActivityStageMappingRepository activityStageMappingRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityRepository = activityRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.activityStageRepository = activityStageRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> assignActivity(Long id, Map<String, Object> body) {
        Activity activity = activityRepository.findById(id).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        Long targetStageId = null;
        if (body != null && body.containsKey("stageId") && body.get("stageId") != null) {
            try {
                targetStageId = Long.valueOf(body.get("stageId").toString());
            } catch (Exception ignored) {}
        }

        ActivityStage targetStage = null;
        List<ActivityAssignment> existingAssignments;
        if (targetStageId != null) {
            targetStage = activityStageRepository.findById(targetStageId).orElse(null);
            existingAssignments = activityAssignmentRepository.findByActivityIdAndStageId(id, targetStageId);
        } else {
            existingAssignments = activityAssignmentRepository.findByActivityId(id);
        }
        
        log.info("DELETE LOG: Method=assignActivity Class=ActivityAssignmentService Reason=Fetching Existing Assignments TotalFound={}", existingAssignments.size());

        boolean ccEnabled = Boolean.TRUE.equals(body.get("ccEnabled"));
        boolean globalEnabled = Boolean.TRUE.equals(body.get("globalEnabled"));

        if (ccEnabled) {
            if (targetStageId != null) {
                ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(targetStageId, id).orElse(null);
                if (mapping != null) {
                    mapping.setAssignmentMode("CLASS_COORDINATOR");
                    activityStageMappingRepository.save(mapping);
                }
            } else {
                activity.setAssignmentMode("CLASS_COORDINATOR");
                activityRepository.save(activity);
            }

            List<Department> allDepts = departmentRepository.findAll();
            List<String> warnings = new ArrayList<>();
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();

            java.util.List<User> allCCs = userRepository.findAllClassCoordinators();
            Map<String, User> ccMap = new java.util.HashMap<>();
            for (User u : allCCs) {
                if (u.getDepartment() != null && u.getSection() != null) {
                    ccMap.put(u.getDepartment().getId() + "_" + u.getSection().getId(), u);
                }
            }

            List<Section> allSections = sectionRepository.findAll();
            Map<Long, List<Section>> sectionsByDept = allSections.stream()
                    .filter(s -> s.getDepartment() != null)
                    .collect(java.util.stream.Collectors.groupingBy(s -> s.getDepartment().getId()));

            for (Department dept : allDepts) {
                List<Section> sections = sectionsByDept.getOrDefault(dept.getId(), new ArrayList<>());
                for (Section sec : sections) {
                    User cc = ccMap.get(dept.getId() + "_" + sec.getId());
                    if (cc == null) {
                        warnings.add("Section " + sec.getSectionName() + " (" + dept.getName()
                                + "): No Class Coordinator assigned");
                        continue;
                    }
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    aa.setStage(targetStage);
                    aa.setAssignmentScope(AssignmentScope.SECTION);
                    aa.setDepartment(dept);
                    aa.setSection(sec);
                    aa.setTeacher(cc);
                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());
                    aa.setYear("1");
                    assignmentsToSave.add(aa);
                }
            }
            syncAssignments(existingAssignments, assignmentsToSave);
            if (!warnings.isEmpty()) {
                log.warn("CC Assignment completed with warnings: {}", warnings);
                logAttendanceEngineLink(activity, assignmentsToSave);
                return ResponseEntity.ok(ApiResponse.ok(
                        "Assigned to sections with class coordinators. Warnings: " + String.join(", ", warnings),
                        null));
            }
            logAttendanceEngineLink(activity, assignmentsToSave);
            return ResponseEntity.ok(ApiResponse.ok("Class Coordinator assignments saved successfully", null));

        } else if (globalEnabled) {
            if (targetStageId != null) {
                ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(targetStageId, id).orElse(null);
                if (mapping != null) {
                    mapping.setAssignmentMode("GLOBAL");
                    activityStageMappingRepository.save(mapping);
                }
            } else {
                activity.setAssignmentMode("GLOBAL");
                activityRepository.save(activity);
            }

            List<ActivityAssignment> globalsExisting = existingAssignments.stream()
                .filter(a -> a.getAssignmentScope() == AssignmentScope.GLOBAL)
                .collect(java.util.stream.Collectors.toList());

            List<Department> allDepts = departmentRepository.findAll();
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();
            for (Department dept : allDepts) {
                ActivityAssignment aa = new ActivityAssignment();
                aa.setActivity(activity);
                aa.setStage(targetStage);
                aa.setAssignmentScope(AssignmentScope.GLOBAL);
                aa.setDepartment(dept);
                aa.setAssignedBy(currentUser);
                aa.setAssignedAt(LocalDateTime.now());
                aa.setYear("1");
                assignmentsToSave.add(aa);
            }
            syncAssignments(globalsExisting, assignmentsToSave);
            logAttendanceEngineLink(activity, assignmentsToSave);
            return ResponseEntity.ok(ApiResponse.ok("Activity successfully assigned globally (Section assignments retained)", null));

        } else {
            // MANUAL ASSIGNMENT MODE

            if (targetStageId != null) {
                ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(targetStageId, id).orElse(null);
                if (mapping != null) {
                    mapping.setAssignmentMode("MANUAL");
                    activityStageMappingRepository.save(mapping);
                }
            } else {
                activity.setAssignmentMode("MANUAL");
                activityRepository.save(activity);
            }

            List<Map<String, Object>> assignmentsList = (List<Map<String, Object>>) body.get("assignments");
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();
            if (assignmentsList != null) {
                for (Map<String, Object> item : assignmentsList) {
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    aa.setStage(targetStage);

                    String scopeStr = (String) item.get("scope");
                    AssignmentScope scope = AssignmentScope.valueOf(scopeStr);
                    aa.setAssignmentScope(scope);
                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());

                    if (scope == AssignmentScope.DEPARTMENT) {
                        Long deptId = ((Number) item.get("departmentId")).longValue();
                        Department dept = departmentRepository.findById(deptId).orElse(null);
                        aa.setDepartment(dept);
                    } else if (scope == AssignmentScope.SECTION) {
                        Long secId = ((Number) item.get("sectionId")).longValue();
                        Section sec = sectionRepository.findById(secId).orElse(null);
                        aa.setSection(sec);
                        if (sec != null)
                            aa.setDepartment(sec.getDepartment());
                    }

                    if (item.containsKey("teacherId") && item.get("teacherId") != null) {
                        Long teacherId = ((Number) item.get("teacherId")).longValue();
                        User teacher = userRepository.findById(teacherId).orElse(null);
                        aa.setTeacher(teacher);
                    }

                    assignmentsToSave.add(aa);
                }
            }
            syncAssignments(existingAssignments, assignmentsToSave);
            logAttendanceEngineLink(activity, assignmentsToSave);
            return ResponseEntity.ok(ApiResponse.ok("Activity assignments updated successfully", null));
        }
    }

    private void logAttendanceEngineLink(Activity activity, List<ActivityAssignment> assignments) {
        if (!Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            return;
        }

        String stageName = activity.getStage() != null ? activity.getStage().getName() : "Unknown";
        String academicYear = activity.getAcademicYear() != null ? activity.getAcademicYear().name() : "Unknown";
        
        List<String> departments = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        List<String> teachers = new ArrayList<>();
        
        for (ActivityAssignment aa : assignments) {
            if (aa.getDepartment() != null) departments.add(aa.getDepartment().getName());
            if (aa.getSection() != null) sections.add(aa.getSection().getSectionName());
            if (aa.getTeacher() != null) teachers.add(aa.getTeacher().getFullName());
        }
        
        String deptsStr = departments.isEmpty() ? "None" : String.join(", ", departments.stream().distinct().collect(java.util.stream.Collectors.toList()));
        String secsStr = sections.isEmpty() ? "None" : String.join(", ", sections.stream().distinct().collect(java.util.stream.Collectors.toList()));
        String teachersStr = teachers.isEmpty() ? "None" : String.join(", ", teachers.stream().distinct().collect(java.util.stream.Collectors.toList()));

        boolean resolvedSuccessfully = activity.getStage() != null && !assignments.isEmpty();
        String reason = resolvedSuccessfully ? "Successfully mapped to Stage and Assignments" : "Missing Stage or Assignments";

        log.info("=============================");
        log.info("ATTENDANCE ENGINE LINK");
        log.info("=============================");
        log.info("Activity ID          : {}", activity.getId());
        log.info("Activity Name        : {}", activity.getName());
        log.info("Attendance Enabled   : {}", activity.getAttendanceEngineEnabled());
        log.info("Stage                : {}", stageName);
        log.info("Academic Year        : {}", academicYear);
        log.info("Assignment Mode      : {}", activity.getAssignmentMode());
        log.info("Departments          : {}", deptsStr);
        log.info("Sections             : {}", secsStr);
        log.info("Teachers             : {}", teachersStr);
        log.info("Resolved Successfully: {}", resolvedSuccessfully ? "YES" : "NO");
        log.info("Reason               : {}", reason);
        log.info("=============================");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ActivityAssignmentResponse>>> getAssignments(Long activityId) {
        return getAssignments(activityId, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ActivityAssignmentResponse>>> getAssignments(Long activityId, Long stageId) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<List<ActivityAssignmentResponse>>error("Activity not found"));
        }

        List<ActivityAssignment> assignments;
        if (stageId != null) {
            assignments = activityAssignmentRepository.findByActivityIdAndStageId(activityId, stageId);
            if (assignments.isEmpty()) {
                assignments = activityAssignmentRepository.findByActivityIdAndStageIdIsNull(activityId);
            }
        } else {
            assignments = activityAssignmentRepository.findByActivityId(activityId);
        }

        log.info("Fetched {} assignments for activityId={}, stageId={}", assignments.size(), activityId, stageId);

        log.info("========================");
        log.info("GET API LOG: /admin/activities/{}/assignments?stageId={}", activityId, stageId);
        log.info("Fetched {} assignments from database.", assignments.size());
        
        for (ActivityAssignment aa : assignments) {
            log.info("Assignment DB Row -> ID: {}, Activity ID: {}, Stage ID: {}, Dept: {}, Sec: {}, Teacher: {}",
                aa.getId(),
                aa.getActivity() != null ? aa.getActivity().getId() : "NULL",
                aa.getStage() != null ? aa.getStage().getId() : "NULL",
                aa.getDepartment() != null ? aa.getDepartment().getId() : "NULL",
                aa.getSection() != null ? aa.getSection().getId() : "NULL",
                aa.getTeacher() != null ? aa.getTeacher().getId() : "NULL"
            );
        }

        List<ActivityAssignmentResponse> response = assignments.stream().map(a -> {
            log.info("DTO MAPPING LOG -> Entity ID: {}", a.getId());
            Long deptId = a.getDepartment() != null ? a.getDepartment().getId() : null;
            Long secId = a.getSection() != null ? a.getSection().getId() : null;
            Long teachId = a.getTeacher() != null ? a.getTeacher().getId() : null;
            String tName = a.getTeacher() != null ? a.getTeacher().getFullName() : null;
            String aScope = a.getAssignmentScope() != null ? a.getAssignmentScope().name() : null;
            
            log.info("DTO Mapping -> teacherId: {}", teachId);
            log.info("DTO Mapping -> teacherName: {}", tName);
            log.info("DTO Mapping -> sectionId: {}", secId);
            log.info("DTO Mapping -> departmentId: {}", deptId);
            log.info("DTO Mapping -> stageId: {}", a.getStage() != null ? a.getStage().getId() : null);
            log.info("DTO Mapping -> mappingId: N/A");
            log.info("DTO Mapping -> assignmentMode: {}", aScope);
            
            return new ActivityAssignmentResponse(
                a.getId(),
                a.getActivity().getId(),
                a.getActivity().getName(),
                deptId,
                a.getDepartment() != null ? a.getDepartment().getName() : null,
                secId,
                a.getSection() != null ? a.getSection().getSectionName() : null,
                teachId,
                tName,
                a.getTeacher() != null ? a.getTeacher().getUsername() : null,
                a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : "System",
                a.getAssignedAt(),
                a.getYear(),
                aScope);
        }).collect(java.util.stream.Collectors.toList());
        log.info("========================");

        return ResponseEntity.ok(ApiResponse.ok("Assignments fetched successfully", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> addAssignment(Long activityId,
            AssignmentRequest request) {
        return addAssignment(activityId, null, request);
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> addAssignment(Long activityId, Long stageId,
            AssignmentRequest request) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ActivityAssignmentResponse>error("Activity not found"));
        }

        ActivityStage targetStage = stageId != null ? activityStageRepository.findById(stageId).orElse(null) : null;

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        // Check for existing assignment for the same department and section
        List<ActivityAssignment> existing = stageId != null
                ? activityAssignmentRepository.findByActivityIdAndStageId(activityId, stageId)
                : activityAssignmentRepository.findByActivityId(activityId);

        ActivityAssignment aa = existing.stream().filter(a -> {
            boolean deptMatch = (a.getDepartment() == null && request.getDepartmentId() == null) ||
                    (a.getDepartment() != null && request.getDepartmentId() != null
                            && a.getDepartment().getId().equals(request.getDepartmentId()));
            boolean secMatch = (a.getSection() == null && request.getSectionId() == null) ||
                    (a.getSection() != null && request.getSectionId() != null
                            && a.getSection().getId().equals(request.getSectionId()));
            boolean teacherMatch = (a.getTeacher() == null && request.getTeacherId() == null) ||
                    (a.getTeacher() != null && request.getTeacherId() != null
                            && a.getTeacher().getId().equals(request.getTeacherId()));
            return deptMatch && secMatch && teacherMatch;
        }).findFirst().orElse(new ActivityAssignment());

        aa.setActivity(activity);
        aa.setStage(targetStage);
        aa.setAssignmentScope(request.getScope());
        aa.setYear(request.getYear() != null ? request.getYear() : "1");
        aa.setAssignedBy(currentUser);
        aa.setAssignedAt(LocalDateTime.now());

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            aa.setDepartment(dept);
        } else {
            aa.setDepartment(null);
        }

        if (request.getSectionId() != null) {
            Section sec = sectionRepository.findById(request.getSectionId()).orElse(null);
            aa.setSection(sec);
        } else {
            aa.setSection(null);
        }

        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId()).orElse(null);
            aa.setTeacher(teacher);
        } else {
            aa.setTeacher(null);
        }

        log.info("========================");
        log.info("SERVICE LOG: Before saving assignment");
        log.info("Loaded Activity = {}", activity != null ? activity.getId() : "NULL");
        log.info("Loaded Stage = {}", targetStage != null ? targetStage.getId() : "NULL");
        
        // Let's load StageActivityMapping just for logging since user asked for it
        if (targetStage != null && activity != null) {
            ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(targetStage.getId(), activity.getId()).orElse(null);
            log.info("Loaded StageActivityMapping = {}", mapping != null ? mapping.getId() : "NULL");
        } else {
            log.info("Loaded StageActivityMapping = NULL (stage or activity is null)");
        }
        
        log.info("Loaded Teacher = {}", aa.getTeacher() != null ? aa.getTeacher().getId() : "NULL");
        log.info("Loaded Department = {}", aa.getDepartment() != null ? aa.getDepartment().getId() : "NULL");
        log.info("Loaded Section = {}", aa.getSection() != null ? aa.getSection().getId() : "NULL");
        
        log.info("REPOSITORY LOG: Before save()");
        log.info("Existing Assignment Count = {}", existing.size());
        log.info("Existing Assignment IDs = {}", existing.stream().map(ActivityAssignment::getId).collect(java.util.stream.Collectors.toList()));
        
        ActivityAssignment saved = activityAssignmentRepository.save(aa);
        log.info("REPOSITORY LOG: After save()");
        log.info("Saved Assignment ID = {}", saved.getId());
        log.info("Database Row -> ID: {}, Activity: {}, Stage: {}, Dept: {}, Sec: {}, Teacher: {}, Scope: {}",
            saved.getId(),
            saved.getActivity() != null ? saved.getActivity().getId() : "NULL",
            saved.getStage() != null ? saved.getStage().getId() : "NULL",
            saved.getDepartment() != null ? saved.getDepartment().getId() : "NULL",
            saved.getSection() != null ? saved.getSection().getId() : "NULL",
            saved.getTeacher() != null ? saved.getTeacher().getId() : "NULL",
            saved.getAssignmentScope()
        );
        log.info("========================");

        ActivityAssignmentResponse resp = new ActivityAssignmentResponse(
                aa.getId(),
                aa.getActivity().getId(),
                aa.getActivity().getName(),
                aa.getDepartment() != null ? aa.getDepartment().getId() : null,
                aa.getDepartment() != null ? aa.getDepartment().getName() : null,
                aa.getSection() != null ? aa.getSection().getId() : null,
                aa.getSection() != null ? aa.getSection().getSectionName() : null,
                aa.getTeacher() != null ? aa.getTeacher().getId() : null,
                aa.getTeacher() != null ? aa.getTeacher().getFullName() : null,
                aa.getTeacher() != null ? aa.getTeacher().getUsername() : null,
                aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : "System",
                aa.getAssignedAt(),
                aa.getYear(),
                aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : null);

        return ResponseEntity.ok(ApiResponse.ok("Assignment added successfully", resp));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeAssignment(Long assignmentId) {
        if (!activityAssignmentRepository.existsById(assignmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Assignment not found"));
        }
        studentActivityXpRepository.deleteByAssignmentId(assignmentId);
        activityAssignmentRepository.deleteById(assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("Assignment removed successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> clearAssignments(Long activityId) {
        return clearAssignments(activityId, null);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> clearAssignments(Long activityId, Long stageId) {
        if (!activityRepository.existsById(activityId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }
        if (stageId != null) {
            activityAssignmentRepository.deleteByActivityIdAndStageId(activityId, stageId);
            ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(stageId, activityId).orElse(null);
            if (mapping != null) {
                mapping.setAssignmentMode(null);
                activityStageMappingRepository.save(mapping);
            }
        } else {
            studentActivityXpRepository.deleteByActivityId(activityId);
            activityAssignmentRepository.deleteByActivityId(activityId);
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null && stageId == null) {
            activity.setAssignmentMode(null);
            activityRepository.save(activity);
        }

        return ResponseEntity.ok(ApiResponse.ok("All faculty assignments removed successfully", null));
    }

    private void syncAssignments(List<ActivityAssignment> existingAssignments, List<ActivityAssignment> incomingAssignments) {
        log.info("========== ASSIGNMENT SYNC ==========");
        log.info("Activity ID : {}", incomingAssignments.isEmpty() ? "N/A" : incomingAssignments.get(0).getActivity().getId());
        log.info("Existing Assignments : {}", existingAssignments.size());
        log.info("Incoming Assignments : {}", incomingAssignments.size());

        int updatedCount = 0;
        int insertedCount = 0;
        int retainedCount = 0;
        int deletedCount = 0;
        int skippedDeletesCount = 0;

        List<ActivityAssignment> toSave = new ArrayList<>();
        List<ActivityAssignment> toDelete = new ArrayList<>();

        List<ActivityAssignment> unhandledExisting = new ArrayList<>(existingAssignments);

        for (ActivityAssignment incoming : incomingAssignments) {
            ActivityAssignment exactMatch = unhandledExisting.stream()
                .filter(e -> isSameAssignment(e, incoming))
                .findFirst().orElse(null);

            if (exactMatch != null) {
                exactMatch.setAssignedBy(incoming.getAssignedBy());
                exactMatch.setAssignedAt(incoming.getAssignedAt());
                exactMatch.setYear(incoming.getYear());
                toSave.add(exactMatch);
                unhandledExisting.remove(exactMatch);
                retainedCount++;
            } else {
                ActivityAssignment similar = unhandledExisting.stream()
                    .filter(e -> e.getAssignmentScope() == incoming.getAssignmentScope())
                    .findFirst().orElse(null);

                if (similar != null) {
                    similar.setDepartment(incoming.getDepartment());
                    similar.setSection(incoming.getSection());
                    similar.setTeacher(incoming.getTeacher());
                    similar.setAssignedBy(incoming.getAssignedBy());
                    similar.setAssignedAt(incoming.getAssignedAt());
                    similar.setYear(incoming.getYear());
                    toSave.add(similar);
                    unhandledExisting.remove(similar);
                    updatedCount++;
                } else {
                    toSave.add(incoming);
                    insertedCount++;
                }
            }
        }

        for (ActivityAssignment e : unhandledExisting) {
            boolean hasXp = studentActivityXpRepository.existsByAssignmentId(e.getId());
            if (hasXp) {
                skippedDeletesCount++;
                log.info("Assignment ID : {}", e.getId());
                log.info("Referenced By : student_activity_xp");
                log.info("Count : >0");
                log.info("Decision : KEEP EXISTING");
            } else {
                toDelete.add(e);
                deletedCount++;
            }
        }

        activityAssignmentRepository.saveAll(toSave);
        if (!toDelete.isEmpty()) {
            activityAssignmentRepository.deleteAll(toDelete);
        }

        log.info("Assignments Updated : {}", updatedCount);
        log.info("Assignments Inserted : {}", insertedCount);
        log.info("Assignments Retained : {}", retainedCount);
        log.info("Assignments Deleted : {}", deletedCount);
        log.info("Skipped Deletes (Has XP Records) : {}", skippedDeletesCount);
        log.info("=====================================");
    }

    private boolean isSameAssignment(ActivityAssignment a, ActivityAssignment b) {
        if (a.getAssignmentScope() != b.getAssignmentScope()) return false;
        if (a.getDepartment() != null ? !a.getDepartment().getId().equals(b.getDepartment() != null ? b.getDepartment().getId() : null) : b.getDepartment() != null) return false;
        if (a.getSection() != null ? !a.getSection().getId().equals(b.getSection() != null ? b.getSection().getId() : null) : b.getSection() != null) return false;
        if (a.getTeacher() != null ? !a.getTeacher().getId().equals(b.getTeacher() != null ? b.getTeacher().getId() : null) : b.getTeacher() != null) return false;
        return true;
    }
}
