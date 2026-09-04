package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.activity.dto.request.*;
import jjcet.PragatiX.modules.activity.dto.response.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.*;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.faculty.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import jjcet.PragatiX.modules.leaderboard.dto.response.LeaderboardStudentResponse;
import java.util.List;

@Service
public class StudentMapper {
    private static final Logger log = LoggerFactory.getLogger(StudentMapper.class);

    private final jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository;
    private final jjcet.PragatiX.repository.StreakRepository streakRepository;

    public StudentMapper(jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository,
            jjcet.PragatiX.repository.StreakRepository streakRepository) {
        this.stageTeamRepository = stageTeamRepository;
        this.streakRepository = streakRepository;
    }

    public StudentResponse toResponse(Student student) {
        return toResponse(student, null);
    }

    public StudentResponse toResponse(Student student, StudentGuardian guardian) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());

        int sStage = student.getCurrentStage() > 0 ? student.getCurrentStage() : (student.getStage() > 0 ? student.getStage() : 1);

        List<jjcet.PragatiX.entity.Streak> studentStreaks = streakRepository != null && student.getRegNo() != null
                ? streakRepository.findByStudentRegNo(student.getRegNo())
                : java.util.Collections.emptyList();
        int maxStreak = studentStreaks.stream()
                .filter(s -> !s.isBroken())
                .mapToInt(jjcet.PragatiX.entity.Streak::getCurrentStreak)
                .max()
                .orElse(0);

        List<jjcet.PragatiX.dto.StreakResponse> mappedStreaks = studentStreaks.stream().map(s -> {
            jjcet.PragatiX.dto.StreakResponse sr = new jjcet.PragatiX.dto.StreakResponse();
            sr.setCurrentStreak(s.getCurrentStreak());
            sr.setIsBroken(s.isBroken());
            sr.setLastUpdated(s.getLastUpdated());
            sr.setStreakType(s.getStreakType());
            sr.setPenaltyPerBreak(s.getPenaltyPerBreak());
            return sr;
        }).collect(java.util.stream.Collectors.toList());

        return StudentResponse.builder()
                .id(student.getId())
                .regNo(student.getRegNo())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone() != null ? student.getPhone() : student.getPhoneNo())
                .gender(student.getGender() != null && !student.getGender().trim().isEmpty()
                        ? student.getGender()
                        : (student.getGenderRef() != null ? student.getGenderRef().getGenderName() : null))
                .genderId(student.getGenderRef() != null ? student.getGenderRef().getId() : null)
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .departmentId(student.getDepartment() != null ? student.getDepartment().getId() : null)
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
                .semester(student.getSemester())
                .semesterId(student.getSemesterRef() != null ? student.getSemesterRef().getId() : null)
                .year(student.getYear())
                .yearId(student.getYearRef() != null ? student.getYearRef().getId() : null)
                .section(student.getSection() != null ? student.getSection().getSectionName() : null)
                .sectionId(student.getSection() != null ? student.getSection().getId() : null)
                .sectionName(student.getSection() != null ? student.getSection().getSectionName() : null)
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
                .guardian(guardian != null ? mapGuardianToDto(guardian) : null)
                .currentStreak(maxStreak)
                .streaks(mappedStreaks)
                .build();
    }

    public StudentSelfResponse toSelfResponse(Student student, StudentGuardian guardian) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;

        int sStage = student.getCurrentStage() > 0 ? student.getCurrentStage() : (student.getStage() > 0 ? student.getStage() : 1);

        List<jjcet.PragatiX.entity.Streak> studentStreaks = streakRepository != null && student.getRegNo() != null
                ? streakRepository.findByStudentRegNo(student.getRegNo())
                : java.util.Collections.emptyList();
        int maxStreak = studentStreaks.stream()
                .filter(s -> !s.isBroken())
                .mapToInt(jjcet.PragatiX.entity.Streak::getCurrentStreak)
                .max()
                .orElse(0);

        List<jjcet.PragatiX.dto.StreakResponse> mappedStreaks = studentStreaks.stream().map(s -> {
            jjcet.PragatiX.dto.StreakResponse sr = new jjcet.PragatiX.dto.StreakResponse();
            sr.setCurrentStreak(s.getCurrentStreak());
            sr.setIsBroken(s.isBroken());
            sr.setLastUpdated(s.getLastUpdated());
            sr.setStreakType(s.getStreakType());
            sr.setPenaltyPerBreak(s.getPenaltyPerBreak());
            return sr;
        }).collect(java.util.stream.Collectors.toList());

        return StudentSelfResponse.builder()
                .id(student.getId())
                .regNo(student.getRegNo())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone() != null ? student.getPhone() : student.getPhoneNo())
                .gender(student.getGender() != null && !student.getGender().trim().isEmpty()
                        ? student.getGender()
                        : (student.getGenderRef() != null ? student.getGenderRef().getGenderName() : null))
                .genderId(student.getGenderRef() != null ? student.getGenderRef().getId() : null)
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .departmentId(student.getDepartment() != null ? student.getDepartment().getId() : null)
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
                .semester(student.getSemester())
                .semesterId(student.getSemesterRef() != null ? student.getSemesterRef().getId() : null)
                .year(student.getYear())
                .yearId(student.getYearRef() != null ? student.getYearRef().getId() : null)
                .section(student.getSection() != null ? student.getSection().getSectionName() : null)
                .sectionId(student.getSection() != null ? student.getSection().getId() : null)
                .sectionName(student.getSection() != null ? student.getSection().getSectionName() : null)
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
                .guardian(guardian != null ? mapGuardianToDto(guardian) : null)
                .currentStreak(maxStreak)
                .streaks(mappedStreaks)
                .build();
    }

    public LeaderboardStudentResponse toLeaderboardResponse(Student student, int rank) {
        String gender = student.getGender() != null && !student.getGender().trim().isEmpty()
                ? student.getGender()
                : (student.getGenderRef() != null ? student.getGenderRef().getGenderName() : null);

        String deptName = student.getDepartment() != null ? student.getDepartment().getName() : null;

        String yearName = student.getYear() != null && !student.getYear().trim().isEmpty()
                ? student.getYear()
                : (student.getYearRef() != null ? student.getYearRef().getYearName() : null);

        String sectionName = student.getSection() != null ? student.getSection().getSectionName() : null;

        return new LeaderboardStudentResponse(
                rank,
                student.getRegNo(),
                student.getFullName(),
                gender,
                deptName,
                yearName,
                sectionName,
                student.getTotalXp(),
                resolveTeamRole(student)
        );
    }

    private String resolveTeamRole(Student student) {
        if (student.getTeam() == null)
            return "MEMBER";

        if (student.getTeam().getCaptain() != null && student.getTeam().getCaptain().getId().equals(student.getId())) {
            return "CAPTAIN";
        }

        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(student.getTeam().getId());
        for (StageTeam st : stageTeams) {
            if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                return "VICE_CAPTAIN";
            }
        }

        return "MEMBER";
    }

    private GuardianDTO mapGuardianToDto(StudentGuardian guardian) {
        GuardianDTO dto = new GuardianDTO();
        dto.setGuardianName(guardian.getGuardianName());
        dto.setRelationship(guardian.getRelationship() != null ? guardian.getRelationship().name() : "");
        dto.setPhoneNo(guardian.getPhoneNo());
        dto.setEmail(guardian.getEmail());
        return dto;
    }

}
