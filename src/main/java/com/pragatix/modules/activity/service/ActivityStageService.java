package com.pragatix.modules.activity.service;

import com.pragatix.modules.activity.dto.request.ActivityStageRequest;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityStage;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.modules.activity.mapper.ActivityStageMapper;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.repository.DisciplineLogRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.entity.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityStageService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStageService.class);

    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityRepository activityRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final ActivityStageMapper activityStageMapper;
    private final StudentRepository studentRepository;
    private final com.pragatix.repository.StageTeamRepository stageTeamRepository;
    private final com.pragatix.repository.ActivityAssignmentRepository assignmentRepo;
    private final com.pragatix.modules.student.repository.StudentActivityXpRepository xpRepo;
    private final com.pragatix.repository.XpTransactionRepository txRepo;
    private final com.pragatix.modules.authentication.repository.UserRepository userRepository;

    public ActivityStageService(ActivityStageRepository activityStageRepository,
            ActivitySubgroupRepository activitySubgroupRepository,
            ActivityRepository activityRepository,
            DisciplineLogRepository disciplineLogRepository,
            ActivityStageMapper activityStageMapper,
            StudentRepository studentRepository,
            com.pragatix.repository.StageTeamRepository stageTeamRepository,
            com.pragatix.repository.ActivityAssignmentRepository assignmentRepo,
            com.pragatix.modules.student.repository.StudentActivityXpRepository xpRepo,
            com.pragatix.repository.XpTransactionRepository txRepo,
            com.pragatix.modules.authentication.repository.UserRepository userRepository) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityRepository = activityRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.activityStageMapper = activityStageMapper;
        this.studentRepository = studentRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.assignmentRepo = assignmentRepo;
        this.xpRepo = xpRepo;
        this.txRepo = txRepo;
        this.userRepository = userRepository;
    }

    @PostConstruct
    @Transactional
    public void cleanupDuplicateSubgroups() {
        List<ActivityStage> allStages = activityStageRepository.findAll();
        for (ActivityStage stage : allStages) {
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            Map<String, ActivitySubgroup> uniqueCategories = new HashMap<>();

            for (ActivitySubgroup sub : subgroups) {
                String cat = sub.getCategory() != null ? sub.getCategory().toLowerCase() : sub.getName().toLowerCase();

                // If it's a known category
                if (cat.contains("must") || cat.contains("individual") || cat.contains("group")) {
                    String baseCat = cat.contains("must") ? "must"
                            : (cat.contains("individual") ? "individual" : "group");

                    if (uniqueCategories.containsKey(baseCat)) {
                        // Found a duplicate! Delete it if it has no activities.
                        List<Activity> activities = activityRepository.findBySubgroupId(sub.getId());
                        if (activities.isEmpty()) {
                            log.info("Deleting empty duplicate subgroup: {} for stage {}", sub.getName(),
                                    stage.getName());
                            activitySubgroupRepository.delete(sub);
                        } else {
                            // If it has activities, move them to the primary subgroup, then delete
                            ActivitySubgroup primary = uniqueCategories.get(baseCat);
                            for (Activity act : activities) {
                                act.setSubgroup(primary);
                                activityRepository.save(act);
                            }
                            log.info("Merged activities and deleting duplicate subgroup: {} for stage {}",
                                    sub.getName(), stage.getName());
                            activitySubgroupRepository.delete(sub);
                        }
                    } else {
                        // Mark as the primary for this category
                        sub.setCategory(baseCat);
                        activitySubgroupRepository.save(sub);
                        uniqueCategories.put(baseCat, sub);
                    }
                }
            }
        }
    }

    @Transactional
    public List<ActivityStageResponse> getAllStages(com.pragatix.enums.AcademicYear requestedYear) {
        System.out.println("Selected Academic Year : " + requestedYear);

        com.pragatix.enums.AcademicYear effectiveYear = requestedYear;
        Long departmentId = null;
        boolean isStudent = false;

        try {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            com.pragatix.entity.User user = userRepository.findByUsername(username).orElse(null);
            
            com.pragatix.entity.Student student = null;
            if (user == null) {
                student = studentRepository.findByRegNo(username).orElse(null);
                if (student == null) {
                    student = studentRepository.findByEmail(username).orElse(null);
                }
            }

            if (user != null) {
                boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
                boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
                if (!isSuperAdmin && isAdmin) {
                    effectiveYear = user.getAcademicYear();
                }
            } else if (student != null) {
                isStudent = true;
                departmentId = student.getDepartment() != null ? student.getDepartment().getId() : null;
                
                // If effectiveYear is still null, try to derive it from the student entity
                if (effectiveYear == null) {
                    effectiveYear = com.pragatix.enums.AcademicYear.fromStudent(student);
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine user for filtering stages", e);
        }

        List<ActivityStage> allStages = activityStageRepository.findAllByOrderByDisplayOrderAsc();
        System.out.println("Rows Before Filter : " + allStages.size());

        final com.pragatix.enums.AcademicYear finalEffectiveYear = effectiveYear;
        List<ActivityStage> stages;
        
        if (finalEffectiveYear != null) {
            stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(finalEffectiveYear);
        } else if (isStudent) {
            // If it's a student and we couldn't resolve an academic year, return an empty list 
            // rather than returning all stages for all years
            stages = new ArrayList<>();
        } else {
            stages = allStages;
        }
        
        System.out.println("Rows After Academic Year Filter : " + stages.size());

        final Long finalDepartmentId = departmentId;
        final boolean finalIsStudent = isStudent;

        List<ActivityStageResponse> responses = stages.stream().map(stage -> {

            ActivityStageResponse response = activityStageMapper.toResponse(stage);

            // Map subgroups
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            
            // If student, filter subgroups by department
            if (finalIsStudent && finalDepartmentId != null) {
                subgroups = subgroups.stream()
                        .filter(sub -> sub.getAssignedDepartment() == null || sub.getAssignedDepartment().getId().equals(finalDepartmentId))
                        .collect(Collectors.toList());
            }
            
            List<com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse> subMaps = subgroups.stream()
                    .map(sub -> {
                        com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse subMap = new com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse();
                        subMap.setId(sub.getId());
                        subMap.setName(formatSubgroupName(sub));
                        subMap.setThreshold(sub.getThreshold());
                        subMap.setAssignedFacultyId(
                                sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                        subMap.setAssignedFacultyName(
                                sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);

                        // Fetch and attach missing nested activity list
                        List<Activity> activities;
                        if (finalEffectiveYear != null) {
                            activities = activityRepository.findBySubgroupIdAndAcademicYear(sub.getId(),
                                    finalEffectiveYear);
                        } else {
                            activities = activityRepository.findBySubgroupId(sub.getId());
                        }
                        List<com.pragatix.modules.activity.dto.response.ActivityResponse> actMaps = activities.stream()
                                .map(act -> {
                                    com.pragatix.modules.activity.dto.response.ActivityResponse actMap = new com.pragatix.modules.activity.dto.response.ActivityResponse();
                                    actMap.setActivityId(act.getId());
                                    actMap.setActivityName(
                                            act.getActivityName() != null ? act.getActivityName() : act.getName());
                                    actMap.setDescription(
                                            act.getActivityDescription() != null ? act.getActivityDescription()
                                                    : act.getDescription());
                                    int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp()
                                            : act.getMaxPoints();
                                    actMap.setRewardXp(rewardXp);
                                    actMap.setFrequency(
                                            act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
                                    actMap.setEvidence(act.getEvidence());
                                    actMap.setManualEvidenceName(act.getManualEvidenceName());

                                    String facultyName = null;
                                    Long facultyId = null;
                                    if (sub.getAssignedFaculty() != null) {
                                        facultyName = sub.getAssignedFaculty().getFullName();
                                        facultyId = sub.getAssignedFaculty().getId();
                                    }
                                    actMap.setFacultyName(facultyName);
                                    actMap.setFacultyId(facultyId);

                                    return actMap;
                                }).collect(Collectors.toList());

                        subMap.setActivities(actMaps);
                        return subMap;
                    }).collect(Collectors.toList());

            response.setSubgroups(subMaps);

            return response;
        }).collect(Collectors.toList());

        System.out.println("Returned : " + responses.size());
        return responses;
    }

    @Transactional
    public Optional<ActivityStageResponse> getStageById(Long id) {
        return activityStageRepository.findById(id).map(stage -> {
            ActivityStageResponse response = activityStageMapper.toResponse(stage);
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse> subMaps = subgroups.stream()
                    .map(sub -> {
                        com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse subMap = new com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse();
                        subMap.setId(sub.getId());
                        subMap.setName(formatSubgroupName(sub));
                        subMap.setThreshold(sub.getThreshold());
                        subMap.setAssignedFacultyId(
                                sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                        subMap.setAssignedFacultyName(
                                sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);

                        // Fetch and attach missing nested activity list
                        List<Activity> activities = activityRepository.findBySubgroupId(sub.getId());
                        List<com.pragatix.modules.activity.dto.response.ActivityResponse> actMaps = activities.stream()
                                .map(act -> {
                                    com.pragatix.modules.activity.dto.response.ActivityResponse actMap = new com.pragatix.modules.activity.dto.response.ActivityResponse();
                                    actMap.setActivityId(act.getId());
                                    actMap.setActivityName(
                                            act.getActivityName() != null ? act.getActivityName() : act.getName());
                                    actMap.setDescription(
                                            act.getActivityDescription() != null ? act.getActivityDescription()
                                                    : act.getDescription());
                                    int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp()
                                            : act.getMaxPoints();
                                    actMap.setRewardXp(rewardXp);
                                    actMap.setFrequency(
                                            act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
                                    actMap.setEvidence(act.getEvidence());
                                    actMap.setManualEvidenceName(act.getManualEvidenceName());

                                    String facultyName = null;
                                    Long facultyId = null;
                                    if (sub.getAssignedFaculty() != null) {
                                        facultyName = sub.getAssignedFaculty().getFullName();
                                        facultyId = sub.getAssignedFaculty().getId();
                                    }
                                    actMap.setFacultyName(facultyName);
                                    actMap.setFacultyId(facultyId);

                                    return actMap;
                                }).collect(Collectors.toList());

                        subMap.setActivities(actMaps);
                        return subMap;
                    }).collect(Collectors.toList());
            response.setSubgroups(subMaps);
            return response;
        });
    }

    private com.pragatix.enums.AcademicYear resolveRoleBasedAcademicYear(
            com.pragatix.enums.AcademicYear requestedYear) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        com.pragatix.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

        boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));

        if (isSuperAdmin) {
            if (requestedYear == null) {
                throw new IllegalArgumentException("Academic Year is required for Super Admin.");
            }
            return requestedYear;
        } else if (isAdmin) {
            if (user.getAcademicYear() == null) {
                throw new IllegalArgumentException("Admin account is not assigned to any Academic Year.");
            }
            return user.getAcademicYear();
        }

        // Fallback
        if (requestedYear != null)
            return requestedYear;
        throw new IllegalArgumentException("Role not authorized or missing Academic Year.");
    }

    @Transactional
    public ActivityStageResponse createStage(ActivityStageRequest request) {
        System.out.println("Incoming Academic Year: " + request.getAcademicYear());
        System.out.println("Incoming Stage Name: " + request.getName());

        com.pragatix.enums.AcademicYear resolvedYear = resolveRoleBasedAcademicYear(request.getAcademicYear());

        validateStage(request, null);

        ActivityStage stage = activityStageMapper.toEntity(request);
        stage.setAcademicYear(resolvedYear); // explicitly set based on role

        System.out.println("Stage Name Before Save: " + stage.getName());
        System.out.println("Academic Year Before Save: " + stage.getAcademicYear());

        ActivityStage saved = activityStageRepository.save(stage);

        // Reload to verify
        ActivityStage reloaded = activityStageRepository.findById(saved.getId()).orElse(saved);
        System.out.println("Stage ID After Save: " + reloaded.getId());
        System.out.println("Stage Name After Save: " + reloaded.getName());
        System.out.println("Academic Year After Save: " + reloaded.getAcademicYear());

        ensureMandatorySubgroups(saved);

        return activityStageMapper.toResponse(saved);
    }

    @Transactional
    public ActivityStageResponse updateStage(Long id, ActivityStageRequest request) {
        ActivityStage stage = activityStageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Stage not found"));

        com.pragatix.enums.AcademicYear resolvedYear = resolveRoleBasedAcademicYear(request.getAcademicYear());

        System.out.println("Incoming Stage ID : " + id);
        System.out.println("Incoming Academic Year : " + request.getAcademicYear());
        System.out.println("Resolved Academic Year : " + resolvedYear);
        System.out.println("Existing Academic Year : " + stage.getAcademicYear());

        validateStage(request, id);

        activityStageMapper.updateEntity(request, stage);
        stage.setAcademicYear(resolvedYear);

        System.out.println("--- BEFORE UPDATE (Entity) ---");
        System.out.println("Stage ID: " + stage.getId());
        System.out.println("Total Threshold: " + stage.getExpectedXp());
        System.out.println("Must Threshold: " + stage.getMustThreshold());
        System.out.println("Individual Threshold: " + stage.getIndividualThreshold());
        System.out.println("Group Threshold: " + stage.getGroupThreshold());

        System.out.println("Stage Year Before Save : " + stage.getAcademicYear());
        ActivityStage saved = activityStageRepository.save(stage);
        System.out.println("Stage Year After Save : " + saved.getAcademicYear());

        System.out.println("--- AFTER UPDATE (Saved Entity) ---");
        System.out.println("Stage ID: " + saved.getId());
        System.out.println("Total Threshold: " + saved.getExpectedXp());
        System.out.println("Must Threshold: " + saved.getMustThreshold());
        System.out.println("Individual Threshold: " + saved.getIndividualThreshold());
        System.out.println("Group Threshold: " + saved.getGroupThreshold());

        // Update threshold values in associated subgroups to keep the Promotion Engine in sync
        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(id);
        for (ActivitySubgroup sub : subgroups) {
            String cat = sub.getCategory() != null ? sub.getCategory().toLowerCase() : "";
            if (cat.contains("must")) {
                sub.setThreshold(saved.getMustThreshold() != null ? saved.getMustThreshold() : 0);
            } else if (cat.contains("individual")) {
                sub.setThreshold(saved.getIndividualThreshold() != null ? saved.getIndividualThreshold() : 0);
            } else if (cat.contains("group")) {
                sub.setThreshold(saved.getGroupThreshold() != null ? saved.getGroupThreshold() : 0);
            }
            activitySubgroupRepository.save(sub);
        }

        if (resolvedYear != null) {
            List<Activity> activities = activityRepository.findByStageId(id);
            for (Activity act : activities) {
                act.setAcademicYear(resolvedYear);
                activityRepository.save(act);
            }
        }

        return activityStageMapper.toResponse(saved);
    }

    @Transactional
    public void deleteStage(Long id) {
        if (!activityStageRepository.existsById(id)) {
            throw new NoSuchElementException("Stage not found");
        }

        // 0. Delete StageTeams referencing this stage
        List<com.pragatix.entity.StageTeam> stageTeams = stageTeamRepository.findByStageId(id);
        stageTeamRepository.deleteAll(stageTeams);

        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(id);

        // 1. Nullify references in DisciplineLog for each subgroup and activity of this
        // stage
        for (ActivitySubgroup sub : subgroups) {
            disciplineLogRepository.nullifySubgroupReferences(sub.getId());
        }

        List<Activity> activities = activityRepository.findByStageId(id);

        // Resolve Activity Dependencies
        for (Activity act : activities) {
            disciplineLogRepository.nullifyActivityReferences(act.getId());
            xpRepo.deleteByActivityId(act.getId());

            // For XpTransaction, there is no deleteByActivityId out of the box, we may need
            // to iterate or fetch
            List<com.pragatix.entity.XpTransaction> txs = txRepo.findAll().stream()
                    .filter(t -> t.getActivity() != null && t.getActivity().getId().equals(act.getId()))
                    .collect(Collectors.toList());
            txRepo.deleteAll(txs);

            List<com.pragatix.entity.ActivityAssignment> assignments = assignmentRepo.findByActivityId(act.getId());
            assignmentRepo.deleteAll(assignments);
        }

        // 2. Delete all Activity records referencing this stage
        activityRepository.deleteAll(activities);

        // 3. Delete subgroups
        activitySubgroupRepository.deleteAll(subgroups);

        // 4. Delete the stage itself
        activityStageRepository.deleteById(id);

        log.debug("Admin deleted stage and its subgroups, activities, and teams: {}", id);
    }

    private void validateStage(ActivityStageRequest request, Long existingId) {
        if (request.getExpectedXp() != null && request.getExpectedXp() < 0) {
            throw new IllegalArgumentException("Expected XP cannot be negative");
        }

        if (existingId == null) {
            if (activityStageRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Stage name already exists");
            }
        } else {
            if (activityStageRepository.existsByNameAndIdNot(request.getName(), existingId)) {
                throw new IllegalArgumentException("Stage name already exists");
            }
        }

        List<ActivityStage> allStages = activityStageRepository.findAll();
        for (ActivityStage other : allStages) {
            if (existingId != null && other.getId().equals(existingId)) {
                continue;
            }
            if (other.getDisplayOrder() == request.getDisplayOrder()) {
                throw new IllegalArgumentException(
                        "Display order " + request.getDisplayOrder() + " is already used by stage: " + other.getName());
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStageReport(Long id) {
        ActivityStage stage = activityStageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Stage not found"));

        List<Student> allStudents = studentRepository.findByActiveTrue();
        int expectedXp = stage.getExpectedXp() != null ? stage.getExpectedXp() : 0;

        long reachedTarget = 0;
        long totalXpAll = 0;

        for (Student s : allStudents) {
            totalXpAll += s.getTotalXp();
            if (expectedXp > 0 && s.getTotalXp() >= expectedXp) {
                reachedTarget++;
            }
        }

        int totalStudents = allStudents.size();
        double avgXp = totalStudents > 0 ? (double) totalXpAll / totalStudents : 0;
        long belowTarget = totalStudents - reachedTarget;
        double completionPercent = totalStudents > 0 ? ((double) reachedTarget / totalStudents) * 100 : 0;

        Map<String, Object> report = new HashMap<>();
        report.put("stageName", stage.getName());
        report.put("expectedXp", expectedXp);
        report.put("averageXp", Math.round(avgXp * 100.0) / 100.0);
        report.put("reachedTarget", reachedTarget);
        report.put("belowTarget", belowTarget);
        report.put("completionPercent", Math.round(completionPercent * 100.0) / 100.0);
        report.put("totalStudents", totalStudents);

        return report;
    }

    public void ensureMandatorySubgroups(ActivityStage stage) {
        List<ActivitySubgroup> existing = activitySubgroupRepository.findByStageId(stage.getId());
        List<String> categories = existing.stream()
                .map(sub -> sub.getCategory() != null ? sub.getCategory().toLowerCase() : "")
                .collect(Collectors.toList());

        if (!categories.contains("must")) {
            ActivitySubgroup must = new ActivitySubgroup();
            must.setStage(stage);
            must.setCategory("must");
            must.setName("Must (Individual)");
            must.setThreshold(stage.getMustThreshold() != null ? stage.getMustThreshold() : 0);
            activitySubgroupRepository.save(must);
        }
        if (!categories.contains("individual")) {
            ActivitySubgroup ind = new ActivitySubgroup();
            ind.setStage(stage);
            ind.setCategory("individual");
            ind.setName("Individual");
            ind.setThreshold(stage.getIndividualThreshold() != null ? stage.getIndividualThreshold() : 0);
            activitySubgroupRepository.save(ind);
        }
        if (!categories.contains("group")) {
            ActivitySubgroup grp = new ActivitySubgroup();
            grp.setStage(stage);
            grp.setCategory("group");
            grp.setName("Groups");
            grp.setThreshold(stage.getGroupThreshold() != null ? stage.getGroupThreshold() : 0);
            activitySubgroupRepository.save(grp);
        }
    }

    private String formatSubgroupName(ActivitySubgroup sub) {
        if (sub.getCategory() != null && !sub.getCategory().trim().isEmpty()) {
            String cat = sub.getCategory().trim();
            if (cat.equalsIgnoreCase("group")) return "Group"; // fallback to singular
            if (cat.equalsIgnoreCase("groups")) return "Group";
            return cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase();
        } else if (sub.getName() != null) {
            String name = sub.getName();
            if (name.toLowerCase().startsWith("must")) {
                return "Must";
            } else if (name.toLowerCase().startsWith("individual")) {
                return "Individual";
            } else if (name.toLowerCase().startsWith("group")) {
                return "Group";
            }
            return name;
        }
        return "Uncategorized";
    }
}
