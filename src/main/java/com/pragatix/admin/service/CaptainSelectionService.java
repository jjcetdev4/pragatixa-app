package com.pragatix.admin.service;

import com.pragatix.entity.ActivityStage;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.repository.TeamRepository;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CaptainSelectionService {
    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final ActivityStageRepository activityStageRepository;
    private final com.pragatix.admin.service.LeadershipSyncService leadershipSyncService;

    public CaptainSelectionService(TeamRepository teamRepository,
            StudentRepository studentRepository,
            ActivityStageRepository activityStageRepository,
            com.pragatix.admin.service.LeadershipSyncService leadershipSyncService) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.activityStageRepository = activityStageRepository;
        this.leadershipSyncService = leadershipSyncService;
    }

    @Transactional
    public void evaluateCaptainPromotion(Student student) {
        // Obsolete, captain selection is now evaluated per team upon promotion/XP award
        if (student.getTeam() != null) {
            evaluateCaptainForTeam(student.getTeam());
        }
    }

    @Transactional
    public void evaluateCaptainForTeam(Team team) {
        if (team == null || team.getMembers() == null || team.getMembers().isEmpty()) {
            return;
        }

        // Find all active eligible members
        List<Student> eligibleMembers = team.getMembers().stream()
                .filter(Student::isActive)
                .collect(Collectors.toList());

        if (eligibleMembers.isEmpty()) {
            leadershipSyncService.syncLeadership(team, null, null);
            return;
        }

        Student currentCaptain = team.getCaptain();
        Student currentViceCaptain = team.getViceCaptain();

        // 1. Remove current Captain/ViceCaptain if they are no longer eligible members of this team
        if (currentCaptain != null && !eligibleMembers.contains(currentCaptain)) {
            currentCaptain = null;
        }
        if (currentViceCaptain != null && !eligibleMembers.contains(currentViceCaptain)) {
            currentViceCaptain = null;
        }

        // 2. If Captain is vacant but Vice is present, promote Vice to Captain
        if (currentCaptain == null && currentViceCaptain != null) {
            currentCaptain = currentViceCaptain;
            currentViceCaptain = null;
        }

        // 3. Sort by 1. Highest XP 2. Earliest timestamp (simulated by lowest ID if timestamp not available)
        eligibleMembers.sort(Comparator.comparingInt(Student::getTotalXp).reversed()
                .thenComparing(Student::getId));

        // 4. Fill vacancies from highest XP members without overwriting existing roles
        for (Student member : eligibleMembers) {
            if (currentCaptain == null) {
                currentCaptain = member;
            } else if (currentViceCaptain == null && !member.getId().equals(currentCaptain.getId())) {
                currentViceCaptain = member;
            }
            
            if (currentCaptain != null && currentViceCaptain != null) {
                break;
            }
        }

        leadershipSyncService.syncLeadership(team, currentCaptain, currentViceCaptain);

        System.out.println("CAPTAIN SELECTION: Evaluated Captain for team: " + team.getName() + " -> " + (currentCaptain != null ? currentCaptain.getRegNo() : "None"));
        if (currentViceCaptain != null) {
            System.out.println("CAPTAIN SELECTION: Evaluated Vice Captain for team: " + team.getName() + " -> " + currentViceCaptain.getRegNo());
        }
    }
}
