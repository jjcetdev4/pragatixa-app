package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.modules.activity.dto.request.*;
import com.spdms.modules.activity.dto.response.*;
import com.spdms.modules.student.dto.request.*;
import com.spdms.modules.student.dto.response.*;
import com.spdms.common.response.ApiResponse;
import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.activity.repository.*;
import com.spdms.modules.faculty.repository.*;
import com.spdms.modules.student.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;

import java.util.Optional;

@Service
public class StudentTeamService {
    private static final Logger log = LoggerFactory.getLogger(StudentTeamService.class);

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;

    public StudentTeamService(StudentRepository studentRepository, TeamRepository teamRepository) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public ApiResponse<Void> promoteToTeamCaptain(Long regNo) {
        Optional<Student> studentOpt = studentRepository.findById(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Team team = student.getTeam();
        if (team == null) {
            String defaultTeamName = student.getFullName().trim() + "'s Team";
            if (teamRepository.existsByName(defaultTeamName)) {
                defaultTeamName = student.getFullName().trim() + " (" + student.getRegNo().trim() + ")'s Team";
            }
            if (teamRepository.existsByName(defaultTeamName)) {
                defaultTeamName = student.getFullName().trim() + " Team " + System.currentTimeMillis();
            }
            
            team = Team.builder()
                    .name(defaultTeamName)
                    .size(10) // Default max size of 10
                    .captain(student)
                    .build();
            team = teamRepository.save(team);
            student.setTeam(team);
            studentRepository.save(student);
        } else {
            team.setCaptain(student);
            teamRepository.save(team);
        }

        return ApiResponse.ok("Student promoted to Captain of team: " + team.getName(), null);
    }

    @Transactional
    public ApiResponse<Void> removeTeamCaptain(Long regNo) {
        Optional<Student> studentOpt = studentRepository.findById(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Team team = student.getTeam();
        if (team == null) {
            return ApiResponse.error("Student is not assigned to any team");
        }

        if (team.getCaptain() == null || !team.getCaptain().getId().equals(student.getId())) {
            return ApiResponse.error("Student is not the Captain of their team");
        }

        team.setCaptain(null);
        teamRepository.save(team);

        return ApiResponse.ok("Student removed from Captain of team: " + team.getName(), null);
    }

}
