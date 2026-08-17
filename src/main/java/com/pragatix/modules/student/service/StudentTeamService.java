package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.activity.dto.request.*;
import jjcet.PragatiX.modules.activity.dto.response.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.faculty.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;

import java.util.Optional;

@Service
public class StudentTeamService {
    private static final Logger log = LoggerFactory.getLogger(StudentTeamService.class);

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public StudentTeamService(StudentRepository studentRepository,
            TeamRepository teamRepository,
            jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.leadershipSyncService = leadershipSyncService;
    }

    @Transactional
    public ApiResponse<Void> promoteToTeamCaptain(Long regNo) {
        Optional<Student> studentOpt = studentRepository.findById(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Team team = student.getTeam();
        if (team == null) {
            String defaultTeamName = student.getFullName().trim() + "'s Team";
            Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
            Long secId = student.getSection() != null ? student.getSection().getId() : null;
            String year = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYear());

            if (teamRepository.existsByTeamNameAndClass(defaultTeamName, deptId, year, secId)) {
                defaultTeamName = student.getFullName().trim() + " (" + student.getRegNo().trim() + ")'s Team";
            }
            if (teamRepository.existsByTeamNameAndClass(defaultTeamName, deptId, year, secId)) {
                defaultTeamName = student.getFullName().trim() + " Team " + System.currentTimeMillis();
            }

            team = Team.builder()
                    .name(defaultTeamName)
                    .size(10) // Default max size of 10
                    .captain(student)
                    .department(student.getDepartment())
                    .year(jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYear()))
                    .section(student.getSection())
                    .build();
            team = teamRepository.save(team);
            student.setTeam(team);
            studentRepository.save(student);
            team.getMembers().add(student);
            teamRepository.save(team);
        } else {
            if (team.getViceCaptain() != null && team.getViceCaptain().getId().equals(student.getId())) {
                team.setViceCaptain(null);
            }
            if (!team.getMembers().contains(student)) {
                team.getMembers().add(student);
            }
            teamRepository.save(team);
        }

        if (entityManager != null) {
            try {
                entityManager.createNativeQuery(
                        "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                "ON DUPLICATE KEY UPDATE team_id = :tid")
                        .setParameter("tid", team.getId())
                        .setParameter("sid", student.getId())
                        .executeUpdate();
            } catch (Exception ignored) {
            }
        }

        leadershipSyncService.syncLeadership(team, student, team.getViceCaptain());

        return ApiResponse.ok("Student promoted to Captain of team: " + team.getName(), null);
    }

    @Transactional
    public ApiResponse<Void> removeTeamCaptain(Long regNo) {
        Optional<Student> studentOpt = studentRepository.findById(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Team team = student.getTeam();
        if (team == null) {
            return ApiResponse.error("Student is not assigned to any team");
        }

        if (team.getCaptain() == null || !team.getCaptain().getId().equals(student.getId())) {
            return ApiResponse.error("Student is not the Captain of their team");
        }

        leadershipSyncService.syncLeadership(team, null, team.getViceCaptain());

        return ApiResponse.ok("Student removed from Captain of team: " + team.getName(), null);
    }

}
