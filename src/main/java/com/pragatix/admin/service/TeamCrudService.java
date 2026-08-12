package com.pragatix.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.CreateTeamRequest;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.*;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.GroupDeletionAuditLogRepository;
import com.pragatix.repository.TeamRemovalRequestRepository;
import com.pragatix.repository.TeamRepository;
import com.pragatix.repository.StageTeamRepository;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.repository.SectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamCrudService {

    private static final Logger log = LoggerFactory.getLogger(TeamCrudService.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final GroupDeletionAuditLogRepository auditLogRepository;
    private final TeamRemovalRequestRepository teamRemovalRequestRepository;
    private final TeamValidationService validationService;
    private final TeamMapper mapper;
    private final StageTeamRepository stageTeamRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TeamCrudService(TeamRepository teamRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            GroupDeletionAuditLogRepository auditLogRepository,
            TeamRemovalRequestRepository teamRemovalRequestRepository,
            TeamValidationService validationService,
            TeamMapper mapper,
            StageTeamRepository stageTeamRepository,
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.auditLogRepository = auditLogRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.validationService = validationService;
        this.mapper = mapper;
        this.stageTeamRepository = stageTeamRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(CreateTeamRequest request, String username) {
        log.debug("Creating Team with name: {}", request.getName());

        Student studentAttempt = studentRepository.findByRegNo(username).orElse(null);
        if (studentAttempt != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Students are not allowed to create groups."));
        }

        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        ActivityAssignment assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = activityAssignmentRepository.findById(request.getAssignmentId()).orElse(null);
        }

        if (!validationService.canCreateTeam(creator, assignment)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                    "Access Denied: Only Assigned Faculty, Class Coordinators (CC), or Admins can create teams."));
        }

        if (request.getCaptainStudentId() == null || request.getCaptainStudentId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Captain Student ID is required."));
        }

        Student captain = studentRepository.findByRegNo(request.getCaptainStudentId()).orElse(null);
        if (captain == null)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
        if (captain.getTeam() != null || !teamRepository.findAllTeamsByStudentId(captain.getId()).isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse.error("Proposed Captain " + captain.getFullName()
                    + " already belongs to an existing team."));

        Long deptId = request.getDepartmentId() != null ? request.getDepartmentId() : (captain.getDepartment() != null ? captain.getDepartment().getId() : null);
        String year = (request.getAcademicYear() != null && !request.getAcademicYear().trim().isEmpty()) ? request.getAcademicYear() : captain.getYear();
        Long sectionId = request.getSectionId() != null ? request.getSectionId() : (captain.getSection() != null ? captain.getSection().getId() : null);

        if (teamRepository.existsByTeamNameAndClass(request.getName(), deptId, year, sectionId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Team name '" + request.getName() + "' already exists in this class."));
        }

        List<Student> members = new ArrayList<>();
        if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
            java.util.Set<String> uniqueMemberIds = new java.util.LinkedHashSet<>();
            for (String sid : request.getMemberStudentIds()) {
                if (sid != null && !sid.trim().isEmpty()) {
                    String cleanSid = sid.trim();
                    if (cleanSid.equalsIgnoreCase(captain.getRegNo().trim())) {
                        continue;
                    }
                    if (!uniqueMemberIds.add(cleanSid)) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("Duplicate student ID found in members list: " + cleanSid));
                    }
                }
            }
            if (!uniqueMemberIds.isEmpty()) {
                List<Student> fetchedMembers = studentRepository.findByRegNoIn(new ArrayList<>(uniqueMemberIds));
                if (fetchedMembers.size() < uniqueMemberIds.size()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("One or more member students not found."));
                }
                for (Student m : fetchedMembers) {
                    if (m.getTeam() != null || !teamRepository.findAllTeamsByStudentId(m.getId()).isEmpty()) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Student " + m.getFullName()
                                + " is already assigned to a team."));
                    }
                    members.add(m);
                }
            }
        }

        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize
                    + " members (including captain) because the team size limit is " + request.getSize() + "."));
        }

        Department department = null;
        if (deptId != null) {
            department = departmentRepository.findById(deptId).orElse(null);
        }
        Section section = null;
        if (sectionId != null) {
            section = sectionRepository.findById(sectionId).orElse(null);
        }

        Team team = Team.builder()
                .name(request.getName())
                .size(request.getSize())
                .captain(captain)
                .department(department)
                .year(year)
                .section(section)
                .createdBy(creator)
                .build();
        Team savedTeam = teamRepository.save(team);
        log.debug("Saved Team: {}", savedTeam.getName());

        captain.setTeam(savedTeam);
        for (Student m : members) {
            m.setTeam(savedTeam);
        }
        List<Student> toSave = new ArrayList<>(members);
        toSave.add(captain);
        studentRepository.saveAll(toSave);

        try {
            if (entityManager != null) {
                for (Student s : toSave) {
                    entityManager.createNativeQuery(
                            "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                            "ON DUPLICATE KEY UPDATE team_id = :tid")
                            .setParameter("tid", savedTeam.getId())
                            .setParameter("sid", s.getId())
                            .executeUpdate();
                }
            }
        } catch (Exception ignored) {}

        List<StudentResponse> studentResponses = new ArrayList<>();
        studentResponses.add(mapper.toStudentResponse(captain));
        for (Student m : members)
            studentResponses.add(mapper.toStudentResponse(m));

        TeamResponse response = mapper.toTeamResponse(savedTeam);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Team created successfully", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(Long id, CreateTeamRequest request) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }

        Long teamDeptId = team.getDepartment() != null ? team.getDepartment().getId() : null;
        String teamYear = team.getYear();
        Long teamSectionId = team.getSection() != null ? team.getSection().getId() : null;

        if (!team.getName().trim().equalsIgnoreCase(request.getName().trim())
                && teamRepository.existsByTeamNameAndClassExcludingId(request.getName(), teamDeptId, teamYear, teamSectionId, team.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Team name '" + request.getName() + "' already exists in this class."));
        }

        team.setName(request.getName());

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream()
                .anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (request.getSize() < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(request.getSize());
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateTeamLimit(Long id, int size) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getCaptain() != null
                && team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (size < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(size);
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team limit updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteTeam(Long teamId, String username) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        boolean hasOtherMembers = team.getMembers().stream()
                .anyMatch(m -> team.getCaptain() == null || !m.getId().equals(team.getCaptain().getId()));

        if (hasOtherMembers) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Cannot delete team because it still contains students."));
        }

        if (team.getCaptain() != null) {
            Student captain = team.getCaptain();
            team.getMembers().remove(captain);
            captain.setTeam(null);
            studentRepository.save(captain);
            team.setCaptain(null);
        }

        if (team.getViceCaptain() != null) {
            Student vc = team.getViceCaptain();
            team.getMembers().remove(vc);
            vc.setTeam(null);
            studentRepository.save(vc);
            team.setViceCaptain(null);
        }

        // Remove StageTeam mappings
        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(teamId);
        stageTeamRepository.deleteAll(stageTeams);

        teamRemovalRequestRepository.deleteAll(teamRemovalRequestRepository.findByTeamId(teamId));

        try {
            if (entityManager != null) {
                entityManager.createNativeQuery("DELETE FROM team_members WHERE team_id = :tid")
                        .setParameter("tid", teamId)
                        .executeUpdate();
            }
        } catch (Exception ignored) {}

        String teamName = team.getName();
        teamRepository.delete(team);

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isAssignedFaculty = false; // We can't determine this globally without an assignment context
        String roleStr = isAdmin ? "ADMIN" : (isAssignedFaculty ? "ASSIGNED_FACULTY" : "CC");

        GroupDeletionAuditLog auditLog = new GroupDeletionAuditLog(
                teamId, teamName, username, roleStr, "User initiated deletion", java.time.LocalDateTime.now());
        auditLogRepository.save(auditLog);
        return ResponseEntity.ok(ApiResponse.ok("Group deleted successfully", null));
    }
}
