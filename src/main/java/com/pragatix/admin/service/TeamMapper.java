package com.pragatix.admin.service;

import com.pragatix.dto.TeamRemovalRequestDto;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.entity.TeamRemovalRequest;
import com.pragatix.modules.student.dto.response.StudentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import com.pragatix.repository.StageTeamRepository;
import com.pragatix.entity.StageTeam;

@Component
public class TeamMapper {

    private final StageTeamRepository stageTeamRepository;

    public TeamMapper(StageTeamRepository stageTeamRepository) {
        this.stageTeamRepository = stageTeamRepository;
    }

    public StudentResponse toStudentResponse(Student student) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());

        return StudentResponse.builder()
                .id(student.getId())
                .regNo(student.getRegNo())
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
                .teamRole(resolveTeamRole(student))
                .build();
    }

    private String resolveTeamRole(Student student) {
        if (student.getTeam() == null)
            return "MEMBER";

        if (student.getTeam().getCaptain() != null && student.getTeam().getCaptain().getId().equals(student.getId())) {
            return "CAPTAIN";
        }

        if (student.getTeam().getViceCaptain() != null
                && student.getTeam().getViceCaptain().getId().equals(student.getId())) {
            return "VICE_CAPTAIN";
        }

        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(student.getTeam().getId());
        for (StageTeam st : stageTeams) {
            if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                return "VICE_CAPTAIN";
            }
        }

        return "MEMBER";
    }

    public TeamResponse toTeamResponse(Team team) {
        List<StudentResponse> studentResponses = team.getMembers().stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());

        String captainId = team.getCaptain() != null ? team.getCaptain().getRegNo() : null;
        String captainName = team.getCaptain() != null ? team.getCaptain().getFullName() : null;

        String viceCaptainId = team.getViceCaptain() != null ? team.getViceCaptain().getRegNo() : null;
        String viceCaptainName = team.getViceCaptain() != null ? team.getViceCaptain().getFullName() : null;

        if (captainId != null) {
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getRegNo().equals(captainId));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(team.getCaptain()));
            }
        }

        if (viceCaptainId != null) {
            boolean viceCaptainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getRegNo().equals(viceCaptainId));
            if (!viceCaptainInMembers) {
                studentResponses.add(toStudentResponse(team.getViceCaptain()));
            }
        }

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getSize(),
                captainId,
                captainName,
                viceCaptainId,
                viceCaptainName,
                studentResponses);

        // Data Resolution Priority
        // 1. Team Entity (if directly stored)
        // 2. Captain
        // 3. First Member

        Student representative = team.getCaptain();
        if (representative == null && !team.getMembers().isEmpty()) {
            representative = team.getMembers().stream()
                    .filter(Student::isActive)
                    .findFirst()
                    .orElse(team.getMembers().iterator().next());
        }

        // Department
        if (team.getDepartment() != null) {
            response.setDepartmentId(team.getDepartment().getId());
            response.setDepartmentName(team.getDepartment().getName());
        } else if (representative != null && representative.getDepartment() != null) {
            response.setDepartmentId(representative.getDepartment().getId());
            response.setDepartmentName(representative.getDepartment().getName());
        }

        // Academic Year
        if (representative != null && representative.getAcademicYearRef() != null) {
            response.setAcademicYearId(representative.getAcademicYearRef().getId());
            response.setAcademicYearName(representative.getAcademicYearRef().getAcademicYear());
        }

        // Year
        if (team.getYear() != null && !team.getYear().isEmpty()) {
            response.setYearName(team.getYear());
        } else if (representative != null && representative.getYearRef() != null) {
            response.setYearId(representative.getYearRef().getId());
            response.setYearName("Year " + representative.getYearRef().getYearNo());
        }

        // Semester
        if (representative != null && representative.getSemesterRef() != null) {
            response.setSemesterId(representative.getSemesterRef().getId());
            response.setSemesterName("Semester " + representative.getSemesterRef().getSemesterNo());
        }

        // Section
        if (team.getSection() != null) {
            response.setSectionId(team.getSection().getId());
            response.setSectionName(team.getSection().getSectionName());
        } else if (representative != null && representative.getSection() != null) {
            response.setSectionId(representative.getSection().getId());
            response.setSectionName(representative.getSection().getSectionName());
        }

        return response;
    }

    public TeamRemovalRequestDto toTeamRemovalRequestDto(TeamRemovalRequest req) {
        return new TeamRemovalRequestDto(
                req.getId(),
                req.getTeam().getId(),
                req.getTeam().getName(),
                req.getStudent().getRegNo(),
                req.getStudent().getFullName(),
                req.getCaptain().getRegNo(),
                req.getCaptain().getFullName(),
                req.getReason(),
                req.getStatus(),
                req.getCreatedAt());
    }
}
