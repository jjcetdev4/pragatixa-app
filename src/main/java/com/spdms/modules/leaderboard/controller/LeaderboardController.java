package com.pragatix.modules.leaderboard.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.leaderboard.service.LeaderboardService;
import com.pragatix.modules.leaderboard.dto.response.FilterOptionsDto;
import com.pragatix.modules.student.dto.response.StudentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getLeaderboard(
            @RequestParam(required = false) Long yearId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(yearId, departmentId, sectionId));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<FilterOptionsDto>> getFilters(
            @RequestParam(required = false) Long yearId,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(leaderboardService.getFilters(yearId, departmentId));
    }
}
