package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.dto.TeamRemovalRequestDto;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.entity.TeamRemovalRequest;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.entity.StageTeam;

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

        int sStage = student.getCurrentStage() > 0 ? student.getCurrentStage() : (student.getStage() > 0 ? student.getStage() : 1);

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
                .totalXp(student.getTotalXp())
                .currentXp(student.getTotalXp())
                .mustXp(student.getMustXp())
                .individualXp(student.getIndividualXp())
                .groupXp(student.getGroupXp())
                .teamId(teamId)
                .teamName(teamName)
                .teamRole(resolveTeamRole(student))
                .currentStage(sStage)
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

        // Sort members by XP descending (highest XP on top)
        studentResponses.sort((s1, s2) -> {
            int xp1 = s1.getTotalXp() > 0 ? s1.getTotalXp() : s1.getCurrentXp();
            int xp2 = s2.getTotalXp() > 0 ? s2.getTotalXp() : s2.getCurrentXp();
            if (xp1 != xp2) {
                return Integer.compare(xp2, xp1); // Descending order
            }
            boolean isCap1 = "CAPTAIN".equalsIgnoreCase(s1.getTeamRole());
            boolean isCap2 = "CAPTAIN".equalsIgnoreCase(s2.getTeamRole());
            if (isCap1 && !isCap2) return -1;
            if (!isCap1 && isCap2) return 1;

            boolean isVc1 = "VICE_CAPTAIN".equalsIgnoreCase(s1.getTeamRole());
            boolean isVc2 = "VICE_CAPTAIN".equalsIgnoreCase(s2.getTeamRole());
            if (isVc1 && !isVc2) return -1;
            if (!isVc1 && isVc2) return 1;

            String n1 = s1.getFullName() != null ? s1.getFullName() : "";
            String n2 = s2.getFullName() != null ? s2.getFullName() : "";
            return n1.compareToIgnoreCase(n2);
        });

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

        int teamStage = 1;
        if (team.getCaptain() != null) {
            int capStage = team.getCaptain().getCurrentStage() > 0 ? team.getCaptain().getCurrentStage()
                    : (team.getCaptain().getStage() > 0 ? team.getCaptain().getStage() : 1);
            teamStage = capStage;
        } else if (representative != null) {
            int repStage = representative.getCurrentStage() > 0 ? representative.getCurrentStage()
                    : (representative.getStage() > 0 ? representative.getStage() : 1);
            teamStage = repStage;
        }
        response.setCurrentStage(teamStage);

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
