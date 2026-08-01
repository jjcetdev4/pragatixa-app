package com.pragatix.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TeamQueryService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final TeamMapper mapper;

    private final com.pragatix.modules.authentication.repository.UserRepository userRepository;
    private final TeamValidationService validationService;
    private final TeamCleanupService teamCleanupService;
    private final com.pragatix.modules.student.service.StudentLevelService studentLevelService;
    private final com.pragatix.repository.StageTeamRepository stageTeamRepository;

    public TeamQueryService(TeamRepository teamRepository, StudentRepository studentRepository, TeamMapper mapper,
            com.pragatix.modules.authentication.repository.UserRepository userRepository,
            TeamValidationService validationService,
            TeamCleanupService teamCleanupService,
            com.pragatix.modules.student.service.StudentLevelService studentLevelService,
            com.pragatix.repository.StageTeamRepository stageTeamRepository) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.teamCleanupService = teamCleanupService;
        this.studentLevelService = studentLevelService;
        this.stageTeamRepository = stageTeamRepository;
    }

    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams(String academicYear, Long departmentId, Long sectionId) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        com.pragatix.entity.User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        List<Team> teams = teamRepository.findFilteredTeams(academicYear, departmentId, sectionId);
        List<TeamResponse> responses = teams.stream()
                .filter(team -> {
                    if (team.getCaptain() == null && (team.getMembers() == null || team.getMembers().isEmpty())) {
                        teamCleanupService.autoDeleteEmptyTeam(team);
                        return false;
                    }
                    try {
                        validationService.validateTeamAccess(currentUser, team);
                        return true;
                    } catch (org.springframework.security.access.AccessDeniedException e) {
                        return false;
                    }
                })
                .map(mapper::toTeamResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam(Student student) {
        Team team = teamRepository.findTeamByStudentId(student.getId()).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", mapper.toTeamResponse(team)));
    }

    public ResponseEntity<ApiResponse<com.pragatix.dto.StudentTeamDetailsResponse>> getMyTeamDetails(Student student) {
        Team team = teamRepository.findTeamByStudentId(student.getId()).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        }

        List<com.pragatix.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());

        com.pragatix.dto.StudentTeamDetailsResponse response = new com.pragatix.dto.StudentTeamDetailsResponse();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setDepartment(team.getDepartment() != null ? team.getDepartment().getName() : "N/A");
        response.setSection(team.getSection() != null ? team.getSection().getSectionName() : "N/A");
        response.setAcademicYear(team.getYear() != null ? team.getYear() : "N/A");
        response.setSemester("N/A");
        response.setCaptainName(team.getCaptain() != null ? team.getCaptain().getFullName() : "N/A");
        response.setMaxTeamSize(team.getSize() > 0 ? team.getSize() : 10);

        String viceCaptainName = team.getViceCaptain() != null ? team.getViceCaptain().getFullName() : "N/A";
        String currentRole = "MEMBER";

        // Process members and calculate XP (deduplicated)
        java.util.Set<Student> uniqueMembers = new java.util.HashSet<>();
        if (team.getCaptain() != null)
            uniqueMembers.add(team.getCaptain());
        if (team.getViceCaptain() != null)
            uniqueMembers.add(team.getViceCaptain());
        if (team.getMembers() != null)
            uniqueMembers.addAll(team.getMembers());

        for (com.pragatix.entity.StageTeam st : stageTeams) {
            if (st.getViceCaptain() != null) {
                uniqueMembers.add(st.getViceCaptain());
                if ("N/A".equals(viceCaptainName)) {
                    viceCaptainName = st.getViceCaptain().getFullName();
                }
            }
        }

        java.util.List<Student> allMembers = new java.util.ArrayList<>(uniqueMembers);

        response.setCurrentMemberCount(allMembers.size());

        List<com.pragatix.dto.TeamMemberRankDto> rankDtos = new java.util.ArrayList<>();
        int totalTeamXp = 0;
        int maxStage = 1;

        for (Student m : allMembers) {
            com.pragatix.modules.student.dto.response.StudentProgressionDto progression = studentLevelService
                    .getStudentProgression(m.getRegNo());
            int xp = progression.getTotalXp();
            int stage = 1;
            String currentLevel = "Explorer";
            if (progression.getUnlockedLevels() != null && !progression.getUnlockedLevels().isEmpty()) {
                com.pragatix.modules.student.dto.response.StudentProgressionDto.LevelDto lastLevel = progression
                        .getUnlockedLevels().get(progression.getUnlockedLevels().size() - 1);
                stage = lastLevel.getStage();
                currentLevel = lastLevel.getTitle() != null ? lastLevel.getTitle() : "Explorer";
            }
            totalTeamXp += xp;
            if (stage > maxStage)
                maxStage = stage;

            String role = "MEMBER";
            if (team.getCaptain() != null && team.getCaptain().getRegNo().equals(m.getRegNo())) {
                role = "CAPTAIN";
            } else if (team.getViceCaptain() != null && team.getViceCaptain().getRegNo().equals(m.getRegNo())) {
                role = "VICE_CAPTAIN";
            } else {
                for (com.pragatix.entity.StageTeam st : stageTeams) {
                    if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(m.getId())) {
                        role = "VICE_CAPTAIN";
                        break;
                    }
                }
            }

            if ("VICE_CAPTAIN".equals(role) && "N/A".equals(viceCaptainName)) {
                viceCaptainName = m.getFullName();
            }
            if (m.getRegNo().equals(student.getRegNo())) {
                currentRole = role;
            }

            rankDtos.add(new com.pragatix.dto.TeamMemberRankDto(
                    null, // Profile Image not explicitly stored in basic entity often, can be null
                    m.getFullName(),
                    m.getRegNo(),
                    role,
                    "Stage " + stage,
                    currentLevel,
                    xp,
                    0 // To be assigned after sorting
            ));
        }

        response.setViceCaptainName(viceCaptainName);
        response.setCurrentStudentRole(currentRole);

        // Sort by XP descending
        rankDtos.sort((a, b) -> Integer.compare(b.getTotalXp(), a.getTotalXp()));

        // Assign rank inside team
        int currentRank = 1;
        for (com.pragatix.dto.TeamMemberRankDto dto : rankDtos) {
            dto.setRankInsideTeam(currentRank++);
        }

        response.setMembers(rankDtos);
        response.setTotalTeamXp(totalTeamXp);
        response.setStage("Stage " + maxStage);
        response.setTeamRank(1); // Placeholder for global rank as discussed

        return ResponseEntity.ok(ApiResponse.ok("Team leaderboard retrieved successfully", response));
    }

    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        com.pragatix.entity.User currentUser = userRepository.findByUsername(username).orElse(null);

        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", mapper.toTeamResponse(team)));
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyClassmates(Student currentStudent) {
        if (currentStudent.getDepartment() == null || currentStudent.getSection() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Student is not assigned to a department and section."));
        }

        List<Student> classmates = studentRepository.findByDepartmentIdAndSectionId(
                currentStudent.getDepartment().getId(),
                currentStudent.getSection().getId());

        List<Map<String, Object>> response = classmates.stream()
                .filter(s -> !s.getId().equals(currentStudent.getId()))
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("regNo", s.getRegNo());
                    map.put("fullName", s.getFullName());
                    map.put("regNo", s.getRegNo());
                    map.put("sprNo", s.getSprNo());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Classmates retrieved successfully", response));
    }
}
