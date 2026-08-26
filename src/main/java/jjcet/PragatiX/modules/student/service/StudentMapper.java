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
import java.util.List;

@Service
public class StudentMapper {
    private static final Logger log = LoggerFactory.getLogger(StudentMapper.class);

    private final jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository;

    public StudentMapper(jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository) {
        this.stageTeamRepository = stageTeamRepository;
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

        return StudentResponse.builder()
                .id(student.getId())
                .regNo(student.getRegNo())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .gender(student.getGender())
                .genderId(student.getGenderRef() != null ? student.getGenderRef().getId() : null)
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .departmentId(student.getDepartment() != null ? student.getDepartment().getId() : null)
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
                .semester(student.getSemester())
                .semesterId(student.getSemesterRef() != null ? student.getSemesterRef().getId() : null)
                .academicYear(student.getAcademicYear())
                .academicYearId(student.getAcademicYearRef() != null ? student.getAcademicYearRef().getId() : null)
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
                .build();
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
        dto.setRelationship(guardian.getRelationship().name());
        dto.setPhoneNo(guardian.getPhoneNo());
        dto.setEmail(guardian.getEmail());
        return dto;
    }

}
