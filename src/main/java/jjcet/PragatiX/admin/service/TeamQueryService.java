package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.TeamRepository;
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

    private final jjcet.PragatiX.modules.authentication.repository.UserRepository userRepository;
    private final TeamValidationService validationService;
    private final TeamCleanupService teamCleanupService;
    private final jjcet.PragatiX.modules.student.service.StudentLevelService studentLevelService;
    private final jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository;
    private final jjcet.PragatiX.modules.student.service.TeamAssignmentService teamAssignmentService;
    private final jjcet.PragatiX.modules.activity.repository.ActivityStageRepository activityStageRepository;

    public TeamQueryService(TeamRepository teamRepository, StudentRepository studentRepository, TeamMapper mapper,
            jjcet.PragatiX.modules.authentication.repository.UserRepository userRepository,
            TeamValidationService validationService,
            TeamCleanupService teamCleanupService,
            jjcet.PragatiX.modules.student.service.StudentLevelService studentLevelService,
            jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository,
            jjcet.PragatiX.modules.student.service.TeamAssignmentService teamAssignmentService,
            jjcet.PragatiX.modules.activity.repository.ActivityStageRepository activityStageRepository) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.teamCleanupService = teamCleanupService;
        this.studentLevelService = studentLevelService;
        this.stageTeamRepository = stageTeamRepository;
        this.teamAssignmentService = teamAssignmentService;
        this.activityStageRepository = activityStageRepository;
    }

    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams(String academicYear, Long departmentId,
            Long sectionId) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        jjcet.PragatiX.entity.User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        boolean isYearAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() != null && (r.getName().equalsIgnoreCase("ROLE_ADMIN") || r.getName().equalsIgnoreCase("ADMIN")))
                && !currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() != null && (r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.getName().equalsIgnoreCase("ROLE_SUPERADMIN") || r.getName().equalsIgnoreCase("SUPER_ADMIN")));

        String effectiveYear = academicYear;
        if (isYearAdmin) {
            String adminYear = jjcet.PragatiX.modules.authentication.security.AuthUtils
                    .getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                effectiveYear = adminYear;
            }
        }

        List<Team> teams = teamRepository.findFilteredTeams(effectiveYear, departmentId, sectionId);
        if (teams.isEmpty() && effectiveYear != null) {
            teams = teamRepository.findFilteredTeams(null, departmentId, sectionId);
        }

        final String yearFilter = effectiveYear;
        List<TeamResponse> responses = teams.stream()
                .filter(team -> {
                    if (yearFilter != null && !yearFilter.equalsIgnoreCase("ALL")) {
                        boolean yearMatch = TeamValidationService.isMatchingYear(yearFilter, team.getYear());
                        if (!yearMatch && team.getCaptain() != null) {
                            if (team.getCaptain().getYearRef() != null) {
                                yearMatch = TeamValidationService.isMatchingYear(yearFilter, String.valueOf(team.getCaptain().getYearRef().getYearNo()));
                            }
                            if (!yearMatch) {
                                yearMatch = TeamValidationService.isMatchingYear(yearFilter, team.getCaptain().getYear());
                            }
                        }
                        if (!yearMatch && team.getMembers() != null && !team.getMembers().isEmpty()) {
                            for (Student m : team.getMembers()) {
                                if (m.getYearRef() != null && TeamValidationService.isMatchingYear(yearFilter, String.valueOf(m.getYearRef().getYearNo()))) {
                                    yearMatch = true;
                                    break;
                                }
                                if (TeamValidationService.isMatchingYear(yearFilter, m.getYear())) {
                                    yearMatch = true;
                                    break;
                                }
                            }
                        }
                        if (!yearMatch) {
                            return false;
                        }
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

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam(Student student) {
        Team team = teamRepository.findTeamByStudentId(student.getId()).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", mapper.toTeamResponse(team)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.StudentTeamDetailsResponse>> getMyTeamDetails(
            Student student) {
        Team team = teamRepository.findTeamByStudentId(student.getId()).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        }

        List<jjcet.PragatiX.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());

        jjcet.PragatiX.dto.StudentTeamDetailsResponse response = new jjcet.PragatiX.dto.StudentTeamDetailsResponse();
        response.setTeamId(team.getId());
        response.setTeamName(team.getName());
        response.setDepartment(team.getDepartment() != null ? team.getDepartment().getName() : "N/A");
        response.setSection(team.getSection() != null ? team.getSection().getSectionName() : "N/A");
        response.setAcademicYear(team.getYear() != null ? team.getYear() : "N/A");
        response.setSemester("N/A");
        if (team.getCaptain() != null) {
            response.setCaptainName(team.getCaptain().getFullName());
            response.setCaptainRegNo(team.getCaptain().getRegNo());
            String cGender = team.getCaptain().getGender() != null ? team.getCaptain().getGender()
                    : (team.getCaptain().getGenderRef() != null ? team.getCaptain().getGenderRef().getGenderName() : null);
            response.setCaptainGender(cGender);
        } else {
            response.setCaptainName("N/A");
        }
        if (team.getViceCaptain() != null) {
            response.setViceCaptainName(team.getViceCaptain().getFullName());
            response.setViceCaptainRegNo(team.getViceCaptain().getRegNo());
            String vcGender = team.getViceCaptain().getGender() != null ? team.getViceCaptain().getGender()
                    : (team.getViceCaptain().getGenderRef() != null ? team.getViceCaptain().getGenderRef().getGenderName() : null);
            response.setViceCaptainGender(vcGender);
        } else {
            response.setViceCaptainName("N/A");
        }
        response.setMaxTeamSize(team.getSize() > 0 ? team.getSize() : 10);

        String currentRole = "MEMBER";

        // Process members and calculate XP (deduplicated)
        java.util.Set<Student> uniqueMembers = new java.util.HashSet<>();
        if (team.getCaptain() != null)
            uniqueMembers.add(team.getCaptain());
        if (team.getViceCaptain() != null)
            uniqueMembers.add(team.getViceCaptain());
        if (team.getMembers() != null)
            uniqueMembers.addAll(team.getMembers());

        for (jjcet.PragatiX.entity.StageTeam st : stageTeams) {
            if (st.getCaptain() != null) {
                uniqueMembers.add(st.getCaptain());
                if ("N/A".equals(response.getCaptainName())) {
                    response.setCaptainName(st.getCaptain().getFullName());
                    response.setCaptainRegNo(st.getCaptain().getRegNo());
                    String cGen = st.getCaptain().getGender() != null ? st.getCaptain().getGender()
                            : (st.getCaptain().getGenderRef() != null ? st.getCaptain().getGenderRef().getGenderName() : null);
                    response.setCaptainGender(cGen);
                }
            }
            if (st.getViceCaptain() != null) {
                uniqueMembers.add(st.getViceCaptain());
                if ("N/A".equals(response.getViceCaptainName())) {
                    response.setViceCaptainName(st.getViceCaptain().getFullName());
                    response.setViceCaptainRegNo(st.getViceCaptain().getRegNo());
                    String vcGen = st.getViceCaptain().getGender() != null ? st.getViceCaptain().getGender()
                            : (st.getViceCaptain().getGenderRef() != null ? st.getViceCaptain().getGenderRef().getGenderName() : null);
                    response.setViceCaptainGender(vcGen);
                }
            }
        }

        java.util.List<Student> allMembers = new java.util.ArrayList<>(uniqueMembers);

        response.setCurrentMemberCount(allMembers.size());

        List<jjcet.PragatiX.dto.TeamMemberRankDto> rankDtos = new java.util.ArrayList<>();
        int totalTeamXp = 0;
        int maxStage = 1;

        for (Student m : allMembers) {
            jjcet.PragatiX.modules.student.dto.response.StudentProgressionDto progression = null;
            try {
                progression = studentLevelService.getStudentProgression(m.getRegNo());
            } catch (Exception ignored) {}

            int xp = progression != null ? progression.getTotalXp() : m.getTotalXp();
            int stage = m.getStage() > 0 ? m.getStage() : 1;
            String currentLevel = (progression != null && progression.getCurrentLevelName() != null)
                    ? progression.getCurrentLevelName()
                    : "Explorer";

            totalTeamXp += xp;
            if (stage > maxStage)
                maxStage = stage;

            String role = "MEMBER";
            if (team.getCaptain() != null && team.getCaptain().getRegNo().equals(m.getRegNo())) {
                role = "CAPTAIN";
            } else if (team.getViceCaptain() != null && team.getViceCaptain().getRegNo().equals(m.getRegNo())) {
                role = "VICE_CAPTAIN";
            } else {
                for (jjcet.PragatiX.entity.StageTeam st : stageTeams) {
                    if (st.getCaptain() != null && st.getCaptain().getId().equals(m.getId())) {
                        role = "CAPTAIN";
                        break;
                    }
                    if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(m.getId())) {
                        role = "VICE_CAPTAIN";
                        break;
                    }
                }
            }

            if ("CAPTAIN".equals(role) && "N/A".equals(response.getCaptainName())) {
                response.setCaptainName(m.getFullName());
                response.setCaptainRegNo(m.getRegNo());
                String cGen = m.getGender() != null ? m.getGender()
                        : (m.getGenderRef() != null ? m.getGenderRef().getGenderName() : null);
                response.setCaptainGender(cGen);
            }
            if ("VICE_CAPTAIN".equals(role) && "N/A".equals(response.getViceCaptainName())) {
                response.setViceCaptainName(m.getFullName());
                response.setViceCaptainRegNo(m.getRegNo());
                String vcGen = m.getGender() != null ? m.getGender()
                        : (m.getGenderRef() != null ? m.getGenderRef().getGenderName() : null);
                response.setViceCaptainGender(vcGen);
            }
            if (m.getRegNo().equals(student.getRegNo())) {
                currentRole = role;
            }

            rankDtos.add(new jjcet.PragatiX.dto.TeamMemberRankDto(
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

        response.setCurrentStudentRole(currentRole);

        // Sort by XP descending
        rankDtos.sort((a, b) -> Integer.compare(b.getTotalXp(), a.getTotalXp()));

        // Assign rank inside team
        int currentRank = 1;
        for (jjcet.PragatiX.dto.TeamMemberRankDto dto : rankDtos) {
            dto.setRankInsideTeam(currentRank++);
        }

        response.setMembers(rankDtos);
        response.setTotalTeamXp(totalTeamXp);
        int activeStage = student.getStage() > 0 ? student.getStage() : maxStage;
        response.setStage("Stage " + activeStage);
        response.setTeamRank(1); // Placeholder for global rank as discussed

        return ResponseEntity.ok(ApiResponse.ok("Team leaderboard retrieved successfully", response));
    }

    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        jjcet.PragatiX.entity.User currentUser = userRepository.findByUsername(username).orElse(null);

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
