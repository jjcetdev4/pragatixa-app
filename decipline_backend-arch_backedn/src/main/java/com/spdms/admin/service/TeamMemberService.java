package com.spdms.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;

    public TeamMemberService(TeamRepository teamRepository, StudentRepository studentRepository) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberToTeam(Long id, String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
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
    public ResponseEntity<ApiResponse<Void>> removeMemberFromTeam(Long id, String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));
        if (team.getCaptain() != null && member.getId().equals(team.getCaptain().getId())) return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove the captain from the team this way"));

        member.setTeam(null);
        studentRepository.save(member);
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> assignTeamCaptain(Long id, String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        Student captain = studentRepository.findByStudentId(studentId).orElse(null);
        if (captain == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        if (captain.getTeam() != null && !captain.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is already assigned to a different team: " + captain.getTeam().getName()));

        captain.setTeam(team);
        studentRepository.save(captain);
        team.setCaptain(captain);
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Student assigned as Team Captain successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberByStudent(Student captain, String studentId) {
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
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
    public ResponseEntity<ApiResponse<Void>> addMemberByCC(Long id, String studentId) {
        return addMemberToTeam(id, studentId);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeMemberByCC(Long id, String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));
        if (team.getCaptain() != null && member.getId().equals(team.getCaptain().getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Cannot directly remove the captain. Reassign captaincy first."));

        member.setTeam(null);
        studentRepository.save(member);
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }
}
