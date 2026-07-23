package com.spdms.modules.activity.service;

import com.spdms.modules.activity.dto.request.ActivityStageRequest;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.modules.activity.mapper.ActivityStageMapper;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import com.spdms.repository.DisciplineLogRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.entity.Student;
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
    private final com.spdms.repository.StageTeamRepository stageTeamRepository;
    private final com.spdms.repository.ActivityAssignmentRepository assignmentRepo;
    private final com.spdms.modules.student.repository.StudentActivityXpRepository xpRepo;
    private final com.spdms.repository.XpTransactionRepository txRepo;

    public ActivityStageService(ActivityStageRepository activityStageRepository,
                                ActivitySubgroupRepository activitySubgroupRepository,
                                ActivityRepository activityRepository,
                                DisciplineLogRepository disciplineLogRepository,
                                ActivityStageMapper activityStageMapper,
                                StudentRepository studentRepository,
                                com.spdms.repository.StageTeamRepository stageTeamRepository,
                                com.spdms.repository.ActivityAssignmentRepository assignmentRepo,
                                com.spdms.modules.student.repository.StudentActivityXpRepository xpRepo,
                                com.spdms.repository.XpTransactionRepository txRepo) {
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
    }

    @PostConstruct
    @Transactional
    public void cleanupDuplicateSubgroups() {
        log.info("Starting cleanup of duplicate subgroups...");
        List<ActivityStage> allStages = activityStageRepository.findAll();
        for (ActivityStage stage : allStages) {
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            Map<String, ActivitySubgroup> uniqueCategories = new HashMap<>();
            
            for (ActivitySubgroup sub : subgroups) {
                String cat = sub.getCategory() != null ? sub.getCategory().toLowerCase() : sub.getName().toLowerCase();
                
                // If it's a known category
                if (cat.contains("must") || cat.contains("individual") || cat.contains("group")) {
                    String baseCat = cat.contains("must") ? "must" : (cat.contains("individual") ? "individual" : "group");
                    
                    if (uniqueCategories.containsKey(baseCat)) {
                        // Found a duplicate! Delete it if it has no activities.
                        List<Activity> activities = activityRepository.findBySubgroupId(sub.getId());
                        if (activities.isEmpty()) {
                            log.info("Deleting empty duplicate subgroup: {} for stage {}", sub.getName(), stage.getName());
                            activitySubgroupRepository.delete(sub);
                        } else {
                            // If it has activities, move them to the primary subgroup, then delete
                            ActivitySubgroup primary = uniqueCategories.get(baseCat);
                            for(Activity act : activities) {
                                act.setSubgroup(primary);
                                activityRepository.save(act);
                            }
                            log.info("Merged activities and deleting duplicate subgroup: {} for stage {}", sub.getName(), stage.getName());
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
        log.info("Finished cleanup of duplicate subgroups.");
    }

    @Transactional
    public List<ActivityStageResponse> getAllStages() {
        List<ActivityStage> stages = activityStageRepository.findAllByOrderByDisplayOrderAsc();

        List<ActivityStageResponse> responses = stages.stream().map(stage -> {

            ActivityStageResponse response = activityStageMapper.toResponse(stage);
            
            // Map subgroups
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<com.spdms.modules.activity.dto.response.ActivitySubgroupResponse> subMaps = subgroups.stream().map(sub -> {
                com.spdms.modules.activity.dto.response.ActivitySubgroupResponse subMap = new com.spdms.modules.activity.dto.response.ActivitySubgroupResponse();
                subMap.setId(sub.getId());
                subMap.setName(sub.getName());
                subMap.setThreshold(sub.getThreshold());
                subMap.setAssignedFacultyId(sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                subMap.setAssignedFacultyName(sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);
                
                // Fetch and attach missing nested activity list
                List<Activity> activities = activityRepository.findBySubgroupId(sub.getId());
                List<com.spdms.modules.activity.dto.response.ActivityResponse> actMaps = activities.stream().map(act -> {
                    com.spdms.modules.activity.dto.response.ActivityResponse actMap = new com.spdms.modules.activity.dto.response.ActivityResponse();
                    actMap.setActivityId(act.getId());
                    actMap.setActivityName(act.getActivityName() != null ? act.getActivityName() : act.getName());
                    actMap.setDescription(act.getActivityDescription() != null ? act.getActivityDescription() : act.getDescription());
                    int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp() : act.getMaxPoints();
                    actMap.setRewardXp(rewardXp);
                    actMap.setFrequency(act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
                    actMap.setEvidence(act.getEvidence());
                    
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

        return responses;
    }

    @Transactional
    public Optional<ActivityStageResponse> getStageById(Long id) {
        return activityStageRepository.findById(id).map(stage -> {
            ActivityStageResponse response = activityStageMapper.toResponse(stage);
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<com.spdms.modules.activity.dto.response.ActivitySubgroupResponse> subMaps = subgroups.stream().map(sub -> {
                com.spdms.modules.activity.dto.response.ActivitySubgroupResponse subMap = new com.spdms.modules.activity.dto.response.ActivitySubgroupResponse();
                subMap.setId(sub.getId());
                subMap.setName(sub.getName());
                subMap.setThreshold(sub.getThreshold());
                subMap.setAssignedFacultyId(sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                subMap.setAssignedFacultyName(sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);
                
                // Fetch and attach missing nested activity list
                List<Activity> activities = activityRepository.findBySubgroupId(sub.getId());
                List<com.spdms.modules.activity.dto.response.ActivityResponse> actMaps = activities.stream().map(act -> {
                    com.spdms.modules.activity.dto.response.ActivityResponse actMap = new com.spdms.modules.activity.dto.response.ActivityResponse();
                    actMap.setActivityId(act.getId());
                    actMap.setActivityName(act.getActivityName() != null ? act.getActivityName() : act.getName());
                    actMap.setDescription(act.getActivityDescription() != null ? act.getActivityDescription() : act.getDescription());
                    int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp() : act.getMaxPoints();
                    actMap.setRewardXp(rewardXp);
                    actMap.setFrequency(act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
                    actMap.setEvidence(act.getEvidence());
                    
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

    @Transactional
    public ActivityStageResponse createStage(ActivityStageRequest request) {
        validateStage(request, null);
        
        ActivityStage stage = activityStageMapper.toEntity(request);
        ActivityStage saved = activityStageRepository.save(stage);
        
        ensureMandatorySubgroups(saved);
        
        return activityStageMapper.toResponse(saved);
    }

    @Transactional
    public ActivityStageResponse updateStage(Long id, ActivityStageRequest request) {
        ActivityStage stage = activityStageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Stage not found"));
        
        validateStage(request, id);
        
        activityStageMapper.updateEntity(request, stage);
        ActivityStage saved = activityStageRepository.save(stage);
        
        return activityStageMapper.toResponse(saved);
    }

    @Transactional
    public void deleteStage(Long id) {
        if (!activityStageRepository.existsById(id)) {
            throw new NoSuchElementException("Stage not found");
        }
        
        // 0. Delete StageTeams referencing this stage
        List<com.spdms.entity.StageTeam> stageTeams = stageTeamRepository.findByStageId(id);
        stageTeamRepository.deleteAll(stageTeams);
        
        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(id);
        
        // 1. Nullify references in DisciplineLog for each subgroup and activity of this stage
        for (ActivitySubgroup sub : subgroups) {
            disciplineLogRepository.nullifySubgroupReferences(sub.getId());
        }
        
        List<Activity> activities = activityRepository.findByStageId(id);
        
        // Resolve Activity Dependencies
        for (Activity act : activities) {
            disciplineLogRepository.nullifyActivityReferences(act.getId());
            xpRepo.deleteByActivityId(act.getId());
            
            // For XpTransaction, there is no deleteByActivityId out of the box, we may need to iterate or fetch
            List<com.spdms.entity.XpTransaction> txs = txRepo.findAll().stream().filter(t -> t.getActivity() != null && t.getActivity().getId().equals(act.getId())).collect(Collectors.toList());
            txRepo.deleteAll(txs);
            
            List<com.spdms.entity.ActivityAssignment> assignments = assignmentRepo.findByActivityId(act.getId());
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
                throw new IllegalArgumentException("Display order " + request.getDisplayOrder() + " is already used by stage: " + other.getName());
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
        List<String> categories = existing.stream().map(sub -> sub.getCategory() != null ? sub.getCategory().toLowerCase() : "").collect(Collectors.toList());
        
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
}
