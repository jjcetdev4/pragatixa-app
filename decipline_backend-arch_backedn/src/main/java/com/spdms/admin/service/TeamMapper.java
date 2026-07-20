package com.spdms.admin.service;

import com.spdms.dto.TeamRemovalRequestDto;
import com.spdms.dto.TeamResponse;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.entity.TeamRemovalRequest;
import com.spdms.modules.student.dto.response.StudentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeamMapper {

    public StudentResponse toStudentResponse(Student student) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());

        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .gender(student.getGender())
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
                .semester(student.getSemester())
                .academicYear(student.getAcademicYear())
                .active(student.isActive())
                .createdAt(student.getCreatedAt())
                .sprNo(student.getSprNo())
                .score(student.getScore())
                .teamId(teamId)
                .teamName(teamName)
                .isCaptain(isCap)
                .build();
    }

    public TeamResponse toTeamResponse(Team team) {
        List<StudentResponse> studentResponses = team.getMembers().stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());

        String captainId = team.getCaptain() != null ? team.getCaptain().getStudentId() : null;
        String captainName = team.getCaptain() != null ? team.getCaptain().getFullName() : null;

        if (captainId != null) {
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getStudentId().equals(captainId));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(team.getCaptain()));
            }
        }

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getSize(),
                captainId,
                captainName,
                studentResponses);
    }

    public TeamRemovalRequestDto toTeamRemovalRequestDto(TeamRemovalRequest req) {
        return new TeamRemovalRequestDto(
                req.getId(),
                req.getTeam().getId(),
                req.getTeam().getName(),
                req.getStudent().getStudentId(),
                req.getStudent().getFullName(),
                req.getCaptain().getStudentId(),
                req.getCaptain().getFullName(),
                req.getReason(),
                req.getStatus(),
                req.getCreatedAt()
        );
    }
}
