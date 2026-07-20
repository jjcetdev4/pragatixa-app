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

    public ActivityStageService(ActivityStageRepository activityStageRepository,
                                ActivitySubgroupRepository activitySubgroupRepository,
                                ActivityRepository activityRepository,
                                DisciplineLogRepository disciplineLogRepository,
                                ActivityStageMapper activityStageMapper,
                                StudentRepository studentRepository) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityRepository = activityRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.activityStageMapper = activityStageMapper;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
        
        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(id);
        
        // 1. Nullify references in DisciplineLog for each subgroup and activity of this stage
        for (ActivitySubgroup sub : subgroups) {
            disciplineLogRepository.nullifySubgroupReferences(sub.getId());
        }
        
        List<Activity> activities = activityRepository.findByStageId(id);
        for (Activity act : activities) {
            disciplineLogRepository.nullifyActivityReferences(act.getId());
        }
        
        // 2. Delete all Activity records referencing this stage
        activityRepository.deleteAll(activities);
        
        // 3. Delete subgroups
        activitySubgroupRepository.deleteAll(subgroups);
        
        // 4. Delete the stage itself
        activityStageRepository.deleteById(id);
        
        log.debug("Admin deleted stage and its subgroups and activities: {}", id);
    }

    private void validateStage(ActivityStageRequest request, Long existingId) {
        if (request.getStartDateTime() == null || request.getEndDateTime() == null) {
            throw new IllegalArgumentException("Start datetime and end datetime are required");
        }
        if (request.getEndDateTime().isBefore(request.getStartDateTime())) {
            throw new IllegalArgumentException("End datetime cannot be before start datetime");
        }
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
            if (other.getStartDateTime() != null && other.getEndDateTime() != null) {
                // Check overlap: S1 < E2 && S2 < E1
                if (request.getStartDateTime().isBefore(other.getEndDateTime()) && other.getStartDateTime().isBefore(request.getEndDateTime())) {
                    throw new IllegalArgumentException("Stage dates overlap with another stage: " + other.getName() + " (" + other.getStartDateTime() + " to " + other.getEndDateTime() + ")");
                }
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
}
