package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.CustomFrequency;
import com.pragatix.modules.activity.dto.response.MyActivityResponse;
import com.pragatix.modules.activity.dto.response.GroupedActivityResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AdminActivityService {

    private final ActivityCrudService crudService;
    private final ActivityAssignmentService assignmentService;
    private final ActivityQueryService queryService;
    private final MyActivityService myActivityService;
    private final ActivityFrequencyService frequencyService;

    public AdminActivityService(ActivityCrudService crudService, ActivityAssignmentService assignmentService,
            ActivityQueryService queryService, MyActivityService myActivityService,
            ActivityFrequencyService frequencyService) {
        this.crudService = crudService;
        this.assignmentService = assignmentService;
        this.queryService = queryService;
        this.myActivityService = myActivityService;
        this.frequencyService = frequencyService;
    }

    public ResponseEntity<ApiResponse<List<MyActivityResponse>>> getMyActivities() {
        return myActivityService.getMyActivities();
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(Long subgroupId,
            com.pragatix.enums.AcademicYear academicYear) {
        return queryService.getActivitiesBySubgroup(subgroupId, academicYear);
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(Long stageId, String subgroup,
            com.pragatix.enums.AcademicYear academicYear) {
        return queryService.getActivitiesByStage(stageId, subgroup, academicYear);
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(String subgroup,
            com.pragatix.enums.AcademicYear academicYear) {
        return queryService.getAllActivities(subgroup, academicYear);
    }

    public ResponseEntity<ApiResponse<List<GroupedActivityResponse>>> getGroupedActivities(Long stageId, String subgroup,
            com.pragatix.enums.AcademicYear academicYear) {
        return queryService.getGroupedActivities(stageId, subgroup, academicYear);
    }

    public ResponseEntity<ApiResponse<Activity>> createActivity(Long subgroupId, Map<String, Object> body) {
        return crudService.createActivity(subgroupId, body);
    }

    public ResponseEntity<ApiResponse<Activity>> updateActivity(Long activityId, Map<String, Object> body) {
        return crudService.updateActivity(activityId, body);
    }

    public ResponseEntity<ApiResponse<Void>> deleteActivity(Long activityId, boolean force) {
        return crudService.deleteActivity(activityId, force);
    }

    public ResponseEntity<ApiResponse<Void>> assignActivity(Long activityId, Map<String, Object> payload) {
        return assignmentService.assignActivity(activityId, payload);
    }

    public ResponseEntity<ApiResponse<List<CustomFrequency>>> getCustomFrequencies() {
        return frequencyService.getCustomFrequencies();
    }

    public ResponseEntity<ApiResponse<CustomFrequency>> createCustomFrequency(Map<String, Object> payload) {
        return frequencyService.createCustomFrequency(payload);
    }

    public ResponseEntity<ApiResponse<Void>> mapActivityToStage(Long stageId, Long activityId, String subgroup) {
        return crudService.mapActivityToStage(stageId, activityId, subgroup);
    }

    public ResponseEntity<ApiResponse<Void>> unmapActivityFromStage(Long stageId, Long activityId) {
        return crudService.unmapActivityFromStage(stageId, activityId);
    }
}
