package com.pragatix.modules.attendance.service;

import com.pragatix.entity.*;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.academiccalendar.repository.AcademicWeekRepository;
import com.pragatix.modules.attendance.repository.CaptainRewardSettingsRepository;
import com.pragatix.repository.StageTeamRepository;
import com.pragatix.repository.TeamRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.XpTransactionRepository;
import com.pragatix.modules.attendancesettings.service.EngineClockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CaptainWeeklyEngineService {

    private static final Logger log = LoggerFactory.getLogger(CaptainWeeklyEngineService.class);

    @Autowired
    private CaptainRewardSettingsRepository settingsRepository;

    @Autowired
    private AcademicWeekRepository weekRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private StageTeamRepository stageTeamRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Autowired
    private EngineClockService clockService;

    @Transactional
    public void execute(AcademicYear year) {
        long startTime = System.currentTimeMillis();

        // 1. Load Reward Settings
        CaptainRewardSettings settings = settingsRepository.findByAcademicYear(year).orElse(null);
        if (settings == null || !Boolean.TRUE.equals(settings.getEngineEnabled())) {
            return;
        }

        LocalDate currentDate = clockService.getEffectiveDate(year);
        LocalTime currentTime = clockService.getEffectiveTime(year);

        // 2. Load Current Academic Week
        AcademicWeek activeWeek = weekRepository.findActiveWeekForDate(year, currentDate).orElse(null);
        if (activeWeek == null || activeWeek.getEndDate() == null) {
            return;
        }

        // 3. Check End Date
        if (!currentDate.isEqual(activeWeek.getEndDate())) {
            return;
        }

        // 4. Check Execution Time
        if (settings.getExecutionTime() != null && currentTime.isBefore(settings.getExecutionTime())) {
            return;
        }

        // 5. Duplicate Detection - Last Execution Date
        if (settings.getLastExecutionDate() != null && settings.getLastExecutionDate().toLocalDate().isEqual(currentDate)) {
            return;
        }

        String weekName = "Week " + (activeWeek.getWeekNumber() != null ? activeWeek.getWeekNumber() : "");
        String captainActivityName = "Captain Weekly Reward - " + weekName;
        String viceCaptainActivityName = "Vice Captain Weekly Reward - " + weekName;

        // Duplicate Detection - Check XP Transactions for today
        boolean alreadyExecuted = xpTransactionRepository.findAll().stream().anyMatch(t -> 
                (captainActivityName.equals(t.getActivityName()) || viceCaptainActivityName.equals(t.getActivityName())) &&
                t.getSubmittedAt().toLocalDate().isEqual(currentDate)
        );

        if (alreadyExecuted) {
            log.info("Captain Reward Engine: Duplicate execution detected for date {}", currentDate);
            settings.setLastExecutionDate(LocalDateTime.now());
            settingsRepository.save(settings);
            return;
        }

        log.info("Captain Reward Engine: Settings Loaded for Academic Year: {}", year);
        log.info("Captain Reward Engine: Academic Week Loaded: {} ({} to {})", weekName, activeWeek.getStartDate(), activeWeek.getEndDate());
        log.info("Captain Reward Engine: Current Week End Date: {}, Current Time: {}", activeWeek.getEndDate(), currentTime);

        int captainXp = settings.getCaptainXp() != null ? settings.getCaptainXp() : 0;
        int viceCaptainXp = settings.getViceCaptainXp() != null ? settings.getViceCaptainXp() : 0;

        // Collect Captains and Vice Captains safely
        Map<Long, Student> captainMap = new LinkedHashMap<>();
        Map<Long, Student> viceCaptainMap = new LinkedHashMap<>();

        // A. From Teams table
        List<Team> allTeams = teamRepository.findAll();
        for (Team team : allTeams) {
            if (team.getCaptain() != null) {
                Student cap = team.getCaptain();
                if (cap.isActive() && AcademicYear.fromStudent(cap) == year) {
                    captainMap.put(cap.getId(), cap);
                }
            }
            if (team.getViceCaptain() != null) {
                Student vc = team.getViceCaptain();
                if (vc.isActive() && AcademicYear.fromStudent(vc) == year) {
                    viceCaptainMap.put(vc.getId(), vc);
                }
            }
        }

        // B. From StageTeam table
        List<StageTeam> allStageTeams = stageTeamRepository.findAll();
        for (StageTeam st : allStageTeams) {
            if (st.getCaptain() != null) {
                Student cap = st.getCaptain();
                if (cap.isActive() && AcademicYear.fromStudent(cap) == year) {
                    captainMap.put(cap.getId(), cap);
                }
            }
            if (st.getViceCaptain() != null) {
                Student vc = st.getViceCaptain();
                if (vc.isActive() && AcademicYear.fromStudent(vc) == year) {
                    viceCaptainMap.put(vc.getId(), vc);
                }
            }
        }

        // C. From Students table (in case student.isCaptain flag or student.team is present)
        List<Student> allStudents = studentRepository.findAll();
        for (Student s : allStudents) {
            if (!s.isActive()) continue;
            if (AcademicYear.fromStudent(s) != year) continue;

            if (s.isCaptain()) {
                captainMap.put(s.getId(), s);
            }
            if (s.getTeam() != null) {
                if (s.getTeam().getCaptain() != null && s.getTeam().getCaptain().getId().equals(s.getId())) {
                    captainMap.put(s.getId(), s);
                }
                if (s.getTeam().getViceCaptain() != null && s.getTeam().getViceCaptain().getId().equals(s.getId())) {
                    viceCaptainMap.put(s.getId(), s);
                }
            }
        }

        // Ensure a student does not get both (Captain takes precedence)
        for (Long capId : captainMap.keySet()) {
            viceCaptainMap.remove(capId);
        }

        log.info("Captains Found : {}", captainMap.size());
        log.info("Vice Captains Found : {}", viceCaptainMap.size());

        List<String> captainLogLines = new ArrayList<>();
        List<String> viceCaptainLogLines = new ArrayList<>();

        int captainsRewarded = 0;
        int viceCaptainsRewarded = 0;
        int totalXpAwarded = 0;

        // Award Captains
        for (Student student : captainMap.values()) {
            try {
                if (captainXp > 0) {
                    student.setTotalXp(student.getTotalXp() + captainXp);
                    student.setScore(student.getScore() + captainXp);
                    studentRepository.save(student);

                    XpTransaction tx = new XpTransaction();
                    tx.setStudent(student);
                    tx.setCategory("LEADERSHIP");
                    tx.setActivityName(captainActivityName);
                    tx.setXpPoints(captainXp);
                    tx.setSubmittedAt(LocalDateTime.now());
                    tx.setStatus("APPROVED");
                    tx.setApprovedBy("Captain Reward Engine");
                    tx.setPenalty(false);
                    tx.setCapApplied(false);
                    tx.setStage(student.getStage());
                    xpTransactionRepository.save(tx);

                    captainsRewarded++;
                    totalXpAwarded += captainXp;
                    captainLogLines.add(String.format("%s\n+%d XP", student.getRegNo() != null ? student.getRegNo() : student.getFullName(), captainXp));
                    log.info("Award Success: Captain {} (ID: {}) awarded {} XP", student.getRegNo(), student.getId(), captainXp);
                }
            } catch (Exception e) {
                log.error("Award Failure for Captain ID {}: {}", student.getId(), e.getMessage());
            }
        }

        // Award Vice Captains
        for (Student student : viceCaptainMap.values()) {
            try {
                if (viceCaptainXp > 0) {
                    student.setTotalXp(student.getTotalXp() + viceCaptainXp);
                    student.setScore(student.getScore() + viceCaptainXp);
                    studentRepository.save(student);

                    XpTransaction tx = new XpTransaction();
                    tx.setStudent(student);
                    tx.setCategory("LEADERSHIP");
                    tx.setActivityName(viceCaptainActivityName);
                    tx.setXpPoints(viceCaptainXp);
                    tx.setSubmittedAt(LocalDateTime.now());
                    tx.setStatus("APPROVED");
                    tx.setApprovedBy("Captain Reward Engine");
                    tx.setPenalty(false);
                    tx.setCapApplied(false);
                    tx.setStage(student.getStage());
                    xpTransactionRepository.save(tx);

                    viceCaptainsRewarded++;
                    totalXpAwarded += viceCaptainXp;
                    viceCaptainLogLines.add(String.format("%s\n+%d XP", student.getRegNo() != null ? student.getRegNo() : student.getFullName(), viceCaptainXp));
                    log.info("Award Success: Vice Captain {} (ID: {}) awarded {} XP", student.getRegNo(), student.getId(), viceCaptainXp);
                }
            } catch (Exception e) {
                log.error("Award Failure for Vice Captain ID {}: {}", student.getId(), e.getMessage());
            }
        }

        settings.setLastExecutionDate(LocalDateTime.now());
        settingsRepository.save(settings);

        long executionTimeMs = System.currentTimeMillis() - startTime;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM");

        StringBuilder report = new StringBuilder();
        report.append("\n=====================================\n");
        report.append("CAPTAIN REWARD ENGINE\n");
        report.append("=====================================\n");
        report.append("Academic Week\n");
        report.append(weekName).append("\n");
        report.append(activeWeek.getStartDate().format(dtf)).append(" - ").append(activeWeek.getEndDate().format(dtf)).append("\n\n");

        if (!captainLogLines.isEmpty()) {
            report.append("Captain\n\n");
            for (String cl : captainLogLines) {
                report.append(cl).append("\n\n");
            }
        }

        if (!viceCaptainLogLines.isEmpty()) {
            report.append("Vice Captain\n\n");
            for (String vcl : viceCaptainLogLines) {
                report.append(vcl).append("\n\n");
            }
        }

        report.append("-------------------------------------\n");
        report.append("SUMMARY\n\n");
        report.append("Captains Rewarded : ").append(captainsRewarded).append("\n");
        report.append("Vice Captains Rewarded : ").append(viceCaptainsRewarded).append("\n");
        report.append("Total XP Awarded : ").append(totalXpAwarded).append("\n");
        report.append("Execution Time : ").append(executionTimeMs).append(" ms\n\n");
        report.append("Captain Reward Engine Completed Successfully\n");
        report.append("=====================================");

        log.info(report.toString());
    }
}
