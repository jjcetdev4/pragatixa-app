package com.spdms.admin;

import com.spdms.dto.ActivityStageRequest;
import com.spdms.dto.ActivityStageResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.mapper.ActivityStageMapper;
import com.spdms.repository.ActivityRepository;
import com.spdms.repository.ActivityStageRepository;
import com.spdms.repository.ActivitySubgroupRepository;
import com.spdms.repository.DisciplineLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
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

    public ActivityStageService(ActivityStageRepository activityStageRepository,
                                ActivitySubgroupRepository activitySubgroupRepository,
                                ActivityRepository activityRepository,
                                DisciplineLogRepository disciplineLogRepository,
                                ActivityStageMapper activityStageMapper) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityRepository = activityRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.activityStageMapper = activityStageMapper;
    }

    @Transactional(readOnly = true)
    public List<ActivityStageResponse> getAllStages() {
        List<ActivityStage> stages = activityStageRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
        return stages.stream().map(stage -> {
            ActivityStageResponse response = activityStageMapper.toResponse(stage);
            
            // Map subgroups
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<Map<String, Object>> subMaps = subgroups.stream().map(sub -> {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", sub.getId());
                subMap.put("name", sub.getName());
                subMap.put("threshold", sub.getThreshold());
                subMap.put("assignedFacultyId", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                subMap.put("assignedFacultyName", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);
                return subMap;
            }).collect(Collectors.toList());
            
            response.setSubgroups(subMaps);
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ActivityStageResponse> getStageById(Long id) {
        return activityStageRepository.findById(id).map(stage -> {
            ActivityStageResponse response = activityStageMapper.toResponse(stage);
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<Map<String, Object>> subMaps = subgroups.stream().map(sub -> {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", sub.getId());
                subMap.put("name", sub.getName());
                subMap.put("threshold", sub.getThreshold());
                subMap.put("assignedFacultyId", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                subMap.put("assignedFacultyName", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);
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
        
        log.info("Admin deleted stage and its subgroups and activities: {}", id);
    }

    private void validateStage(ActivityStageRequest request, Long existingId) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        // Check duplicate name
        if (existingId == null) {
            if (activityStageRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Stage name already exists");
            }
        } else {
            if (activityStageRepository.existsByNameAndIdNot(request.getName(), existingId)) {
                throw new IllegalArgumentException("Stage name already exists");
            }
        }
        
        if (request.isActive()) {
            List<ActivityStage> activeStages = activityStageRepository.findByIsActiveTrue();
            for (ActivityStage other : activeStages) {
                if (existingId != null && other.getId().equals(existingId)) {
                    continue;
                }
                if (other.getStartDate() != null && other.getEndDate() != null) {
                    // Check overlap: S1 <= E2 && S2 <= E1
                    if (!request.getStartDate().isAfter(other.getEndDate()) && !other.getStartDate().isAfter(request.getEndDate())) {
                        throw new IllegalArgumentException("Stage dates overlap with another active stage: " + other.getName() + " (" + other.getStartDate() + " to " + other.getEndDate() + ")");
                    }
                }
            }
        }
    }
}
