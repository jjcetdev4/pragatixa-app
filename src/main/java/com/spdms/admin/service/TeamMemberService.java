package com.spdms.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.TeamRepository;
import com.spdms.enums.TeamRole;
import com.spdms.entity.User;
import com.spdms.modules.authentication.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    private final TeamValidationService validationService;
    private final com.spdms.admin.service.CaptainSelectionService captainSelectionService;
    private final com.spdms.admin.service.TeamMapper teamMapper;
    private final com.spdms.repository.StageTeamRepository stageTeamRepository;
    private final com.spdms.admin.service.TeamCleanupService teamCleanupService;

    public TeamMemberService(TeamRepository teamRepository, 
                             StudentRepository studentRepository, 
                             UserRepository userRepository, 
                             TeamValidationService validationService,
                             com.spdms.admin.service.CaptainSelectionService captainSelectionService,
                             com.spdms.admin.service.TeamMapper teamMapper,
                             com.spdms.repository.StageTeamRepository stageTeamRepository,
                             com.spdms.admin.service.TeamCleanupService teamCleanupService) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.captainSelectionService = captainSelectionService;
        this.teamMapper = teamMapper;
        this.stageTeamRepository = stageTeamRepository;
        this.teamCleanupService = teamCleanupService;
    }


    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberToTeam(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        
        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() != null) return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in team: " + member.getTeam().getName()));

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream().anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        
        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<com.spdms.dto.TeamResponse>> removeMemberFromTeam(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        
        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));
        if (team.getCaptain() != null && member.getId().equals(team.getCaptain().getId())) {
            long nonCaptainMembers = team.getMembers().stream()
                    .filter(m -> !m.getId().equals(team.getCaptain().getId()))
                    .count();
            if (nonCaptainMembers > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Assign another Captain before removing the current Captain."));
            } else {
                team.getMembers().remove(member);
                member.setTeam(null);
                studentRepository.save(member);
                
                team.setCaptain(null);
                
                if (teamCleanupService.autoDeleteEmptyTeam(team)) {
                    return ResponseEntity.ok(ApiResponse.ok("Member removed successfully and empty team auto-deleted", null));
                }
                
                teamRepository.save(team);
                
                return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", teamMapper.toTeamResponse(team)));
            }
        }

        team.getMembers().remove(member);
        member.setTeam(null);
        studentRepository.save(member);
        
        if (teamCleanupService.autoDeleteEmptyTeam(team)) {
            return ResponseEntity.ok(ApiResponse.ok("Member removed successfully and empty team auto-deleted", null));
        }
        
        teamRepository.save(team);
        
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", teamMapper.toTeamResponse(team)));
    }



    @Transactional
    public ResponseEntity<ApiResponse<com.spdms.dto.TeamResponse>> assignTeamCaptain(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        
        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student captain = studentRepository.findByRegNo(regNo).orElse(null);
        if (captain == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (captain.getTeam() != null && !captain.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is already assigned to a different team: " + captain.getTeam().getName()));

        captain.setTeam(team);
        studentRepository.save(captain);
        team.setCaptain(captain);
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Student assigned as Team Captain successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberByStudent(Student captain, String regNo) {
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() != null) return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in team: " + member.getTeam().getName()));

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberByCC(Long id, String regNo) {
        return addMemberToTeam(id, regNo);
    }

    @Transactional
    public ResponseEntity<ApiResponse<com.spdms.dto.TeamResponse>> removeMemberByCC(Long id, String regNo) {
        return removeMemberFromTeam(id, regNo);
    }

}
