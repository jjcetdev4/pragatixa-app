package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.modules.activity.dto.response.ActivityStageResponse;
import jjcet.PragatiX.modules.activity.dto.response.ActivitySubgroupResponse;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.activity.service.ActivityStageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentStageFacade {

    private final ActivityStageService activityStageService;
    private final ActivityRepository activityRepository;
    private final StudentAssignmentResolver assignmentResolver;
    private final StudentStageAssembler stageAssembler;
    private final jjcet.PragatiX.modules.activity.repository.ActivityStageMappingRepository activityStageMappingRepository;

    public StudentStageFacade(ActivityStageService activityStageService,
            ActivityRepository activityRepository,
            StudentAssignmentResolver assignmentResolver,
            StudentStageAssembler stageAssembler,
            jjcet.PragatiX.modules.activity.repository.ActivityStageMappingRepository activityStageMappingRepository) {
        this.activityStageService = activityStageService;
        this.activityRepository = activityRepository;
        this.assignmentResolver = assignmentResolver;
        this.stageAssembler = stageAssembler;
        this.activityStageMappingRepository = activityStageMappingRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getStudentStages(Student student) {
        try {
            jjcet.PragatiX.enums.AcademicYear acYear = jjcet.PragatiX.enums.AcademicYear.fromStudent(student);
            List<ActivityStageResponse> stages = activityStageService.getAllStages(acYear);

            if (stages != null && !stages.isEmpty()) {
                List<Long> subgroupIds = new ArrayList<>();
                for (ActivityStageResponse stage : stages) {
                    if (stage.getSubgroups() != null) {
                        for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                            subgroupIds.add(subgroup.getId());
                        }
                    }
                }

                Map<Long, List<Activity>> activitiesBySubgroup = new HashMap<>();
                List<Long> allActivityIds = new ArrayList<>();
                if (!subgroupIds.isEmpty()) {
                    List<Activity> allActivities = activityRepository.findBySubgroupIdIn(subgroupIds);
                    for (Activity act : allActivities) {
                        if (act.getSubgroup() != null) {
                            activitiesBySubgroup.computeIfAbsent(act.getSubgroup().getId(), k -> new ArrayList<>())
                                    .add(act);
                        }
                        if (!allActivityIds.contains(act.getId())) {
                            allActivityIds.add(act.getId());
                        }
                    }

                    // Also fetch activities mapped to these subgroups via ActivityStageMapping
                    List<jjcet.PragatiX.entity.ActivityStageMapping> allMappings = activityStageMappingRepository
                            .findAll();
                    for (jjcet.PragatiX.entity.ActivityStageMapping mapping : allMappings) {
                        if (mapping.getSubgroup() != null && subgroupIds.contains(mapping.getSubgroup().getId())) {
                            if (mapping.getActivity() != null) {
                                List<Activity> subActs = activitiesBySubgroup
                                        .computeIfAbsent(mapping.getSubgroup().getId(), k -> new ArrayList<>());
                                boolean exists = subActs.stream()
                                        .anyMatch(a -> a.getId().equals(mapping.getActivity().getId()));
                                if (!exists) {
                                    subActs.add(mapping.getActivity());
                                }
                                if (!allActivityIds.contains(mapping.getActivity().getId())) {
                                    allActivityIds.add(mapping.getActivity().getId());
                                }
                            }
                        }
                    }
                }

                Map<Long, List<ActivityAssignment>> assignmentsByActivity = assignmentResolver
                        .fetchAssignmentsByActivity(allActivityIds);
                stageAssembler.assembleStages(student, stages, activitiesBySubgroup, assignmentsByActivity);
            }
            return ResponseEntity.ok(ApiResponse.ok(stages));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch student stages", e);
        }
    }
}
