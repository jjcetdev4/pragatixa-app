package com.pragatix.modules.attendance.service;

import com.pragatix.entity.Attendance;
import com.pragatix.entity.Student;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver;
import com.pragatix.modules.attendance.repository.AttendanceRepository;
import com.pragatix.modules.attendancesettings.repository.AttendanceSettingsRepository;
import com.pragatix.modules.attendancesettings.service.EngineClockService;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.YearRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.entity.Activity;
import com.pragatix.entity.AcademicWeek;
import com.pragatix.modules.academiccalendar.repository.AcademicWeekRepository;
import com.pragatix.modules.student.service.XpEngineService;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.repository.XpTransactionRepository;
import com.pragatix.entity.StudentActivityXp;
import com.pragatix.entity.AttendanceSettings;

import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;
import com.pragatix.entity.ActivityStageMapping;

import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.ActivityStage;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.entity.AssignmentScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AttendanceWeeklyEngineService — executes the Weekly Attendance Engine for a given Academic Year.
 *
 * Uses EngineClockService to respect Production vs Test Mode.
 * Logs detailed results per student.
 * XP reward transactions will be applied in the next XP Engine step.
 */
@Service
public class AttendanceWeeklyEngineService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceWeeklyEngineService.class);

    @Autowired private EngineClockService clockService;
    @Autowired private AcademicCalendarResolver calendarResolver;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private AttendanceSettingsRepository settingsRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private YearRepository yearRepository;
    @Autowired private ActivityRepository activityRepository;
    @Autowired private ActivityStageRepository activityStageRepository;
    @Autowired private ActivityAssignmentRepository activityAssignmentRepository;
    @Autowired private XpEngineService xpEngineService;
    @Autowired private StudentActivityXpRepository studentActivityXpRepository;
    @Autowired private XpTransactionRepository xpTransactionRepository;

    @Autowired private ActivityStageMappingRepository activityStageMappingRepository;
    @Autowired private AcademicWeekRepository academicWeekRepository;



    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear) {
        long startTime = System.currentTimeMillis();
        LocalDate engineDate = clockService.getEffectiveDate(academicYear);
        boolean testMode = clockService.isTestMode(academicYear);

        AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
        if (settings == null) {
            log.info("Attendance Settings not found for year. Aborting.");
            return buildResult("ERROR", "Settings not found", 0, 0, 0, 0, 0);
        }
        
        AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate).orElse(null);
        
        if (activeWeek == null) {
            log.info("No active Academic Week configured. Aborting.");
            return buildResult("ERROR", "No active Academic Week configured", 0, 0, 0, 0, 0);
        }
        
        LocalDate startDate = activeWeek.getStartDate();
        LocalDate endDate = activeWeek.getEndDate();

        if (!engineDate.isEqual(endDate)) {
            log.info("Weekly engine skipped because Engine Date {} is not equal to End Date {}", engineDate, endDate);
            return buildResult("SKIPPED", "Engine Date is not End Date", 0, 0, 0, 0, 0);
        }

        log.info("==================================");
        log.info("ATTENDANCE DATE CONFIGURATION");
        log.info("==================================");
        log.info("Academic Week         : {}", activeWeek.getAcademicMonth().getAcademicYearEnum());
        log.info("Week Number           : {}", activeWeek.getWeekNumber());
        log.info("Week Start Date       : {}", startDate);
        log.info("Week End Date         : {}", endDate);
        log.info("Current Engine Date   : {}", engineDate);
        log.info("Boundary Detected     : WEEK_END");
        log.info("==================================");

        // Count working days in the week
        List<LocalDate> workingDays = new ArrayList<>();
        int holidayCount = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (calendarResolver.isWorkingDay(d, academicYear)) {
                workingDays.add(d);
            } else {
                holidayCount++;
            }
        }

        log.info("====================================================");
        log.info("WEEKLY ATTENDANCE ENGINE STARTED");
        log.info("====================================================");
        log.info("Academic Year  : {}", academicYear);
        log.info("Engine Mode    : {}", testMode ? "TEST" : "PRODUCTION");
        log.info("Week           : {} to {}", startDate, endDate);
        log.info("Working Days   : {}", workingDays.size());
        log.info("Holiday Count  : {}", holidayCount);

        // Resolve yearId
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            log.warn("Could not resolve Year entity for {}", academicYear);
            updateEngineStatus(academicYear, "ERROR", null);
            return buildResult("ERROR", "Could not resolve Year entity.", 0, 0, 0, 1, System.currentTimeMillis() - startTime);
        }

        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        log.info("Students       : {}", allStudents.size());
        
        List<ActivityStage> stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(academicYear);
        int rewarded = 0;
        int notEligible = 0;
        int errors = 0;
        
        // Get settings for perfect week reward
        int perfectWeekReward = settingsRepository.findByAcademicYear(academicYear)
                .map(s -> s.getPerfectWeekReward() != null ? s.getPerfectWeekReward() : 0)
                .orElse(0);

        for (ActivityStage stage : stages) {
            log.info("====================================================");
            log.info("WEEKLY ENGINE START");
            log.info("====================================================");
            log.info("Academic Year : {}", academicYear);
            log.info("Current Stage : {}", stage.getName());


            // --- TEMPORARY FORENSIC LOGS (Safe to remove later) ---
            log.info("==================================================");
            log.info("ATTENDANCE ENGINE RESOLUTION (USING MAPPINGS)");
            log.info("Current Stage           : {} (ID: {})", stage.getName(), stage.getId());
            
            List<ActivityStageMapping> mappings = activityStageMappingRepository.findByStageId(stage.getId());
            log.info("Mapped Stages for Activity (Count): {}", mappings.size());

            Activity engineActivity = null;
            
            for (ActivityStageMapping mapping : mappings) {
                Activity act = mapping.getActivity();
                log.info("--------------------------------------------------");
                log.info("Activity ID             : {}", act.getId());
                log.info("Activity Name           : {}", act.getName());
                log.info("Attendance Enabled      : {}", act.getAttendanceEngineEnabled());
                
                boolean included = true;
                String reason = "";
                
                if (!Boolean.TRUE.equals(act.getAttendanceEngineEnabled())) {
                    included = false;
                    reason = "Skipped because attendanceEngineEnabled=false";
                } else if (!"ACTIVE".equals(act.getStatus())) {
                    included = false;
                    reason = "Skipped because Activity is not ACTIVE (" + act.getStatus() + ")";
                } else if (!academicYear.equals(act.getAcademicYear())) {
                    included = false;
                    reason = "Skipped because Academic Year mismatch";
                }
                
                log.info("Stage Match Result      : SUCCESS (Found in Mappings)");
                log.info("Reason {}         : {}", included ? "Included" : "Excluded", included ? "All conditions met" : reason);
                
                if (included && engineActivity == null) {
                    engineActivity = act;
                }
            }
            log.info("==================================================");
            // --- END FORENSIC LOGS ---

if (engineActivity == null) {
                log.info("Attendance Engine skipped.");
                log.info("Reason: No Attendance Engine activity assigned");
                continue;
            }

            if (!Boolean.TRUE.equals(engineActivity.getAttendanceEngineEnabled())) {
                log.info("Activity assigned");
                log.info("Attendance Engine disabled");
                log.info("Skipping processing.");
                continue;
            }

            String rule = engineActivity.getAttendanceRule();
            if (!"WEEKLY".equals(rule) && !"BOTH".equals(rule)) {
                log.info("Attendance Engine skipped.");
                log.info("Reason: Activity Rule is {}, skipping weekly XP reward.", rule);
                continue;
            }

            List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(engineActivity.getId());
            if (assignments.isEmpty()) {
                log.info("Attendance Engine skipped.");
                log.info("Reason: Activity has no assignments.");
                continue;
            }

            List<String> depts = new java.util.ArrayList<>();
            List<String> secs = new java.util.ArrayList<>();
            for (ActivityAssignment aa : assignments) {
                if (aa.getDepartment() != null) depts.add(aa.getDepartment().getName());
                if (aa.getSection() != null) secs.add(aa.getSection().getSectionName());
            }
            String deptsStr = depts.isEmpty() ? "All/Global" : String.join(", ", depts.stream().distinct().toList());
            String secsStr = secs.isEmpty() ? "All/Global" : String.join(", ", secs.stream().distinct().toList());

            log.info("Resolved Attendance Activity ID : {}", engineActivity.getId());
            log.info("Resolved Activity Name        : {}", engineActivity.getName());
            log.info("Assignment Found              : YES");
            log.info("Department                    : {}", deptsStr);
            log.info("Section                       : {}", secsStr);
            log.info("Processing Started");
            log.info("====================================================");

            int perfectReward = settings.getPerfectWeekReward() != null ? settings.getPerfectWeekReward() : 0;
            for (Student student : allStudents) {
                if (student.getStage() != stage.getDisplayOrder()) continue;

                boolean matchesAssignment = false;
                for (ActivityAssignment aa : assignments) {
                    if (aa.getAssignmentScope() == AssignmentScope.GLOBAL) {
                        matchesAssignment = true; break;
                    } else if (aa.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
                        if (student.getDepartment() != null && student.getDepartment().getId().equals(aa.getDepartment().getId())) {
                            matchesAssignment = true; break;
                        }
                    } else if (aa.getAssignmentScope() == AssignmentScope.SECTION) {
                        if (student.getSection() != null && student.getSection().getId().equals(aa.getSection().getId())) {
                            matchesAssignment = true; break;
                        }
                    }
                }

                if (!matchesAssignment) {
                    continue;
                }

                try {
                    long totalPresent = 0;
                    long totalAbsent = 0;
                    long totalMarked = 0;

                    for (LocalDate workDay : workingDays) {
                        totalPresent += attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                student.getId(), workDay, Attendance.AttendanceStatus.PRESENT);
                        totalAbsent += attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                student.getId(), workDay, Attendance.AttendanceStatus.ABSENT);
                        totalMarked += attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(), workDay);
                    }

                    if (totalMarked == 0) {
                        notEligible++;
                        continue;
                    }

                    double attendancePct = totalMarked == 0 ? 0 : (totalPresent * 100.0 / totalMarked);
                    // Master Prompt: "If the student has even one Partial Absent OR Full Day Absent, No reward."
                    boolean perfectWeek = totalAbsent == 0 && totalPresent == totalMarked && totalMarked == workingDays.size();

                    log.info("==================================== ATTENDANCE XP EXECUTION ====================================");
                    log.info("Student ID         : {}", student.getId());
                    log.info("Student Name       : {}", student.getUser() != null ? student.getUser().getFullName() : student.getRegNo());
                    log.info("Activity ID        : {}", engineActivity.getId());
                    log.info("Activity Name      : {}", engineActivity.getName());
                    log.info("Attendance Status  : {}%", String.format("%.1f", attendancePct));
                    log.info("Perfect Week       : {}", perfectWeek ? "YES" : "NO");
                    
                    if (perfectWeek) {
                        // Duplicate Protection
                        String transactionRemark = "Weekly Reward: " + startDate + " to " + endDate;
                        boolean alreadyProcessed = false;
                        List<com.pragatix.entity.XpTransaction> existingXp = xpTransactionRepository.findByStudentIdAndActivityId(student.getId(), engineActivity.getId());
                        for (com.pragatix.entity.XpTransaction xp : existingXp) {
                            if (xp.getActivityName() != null && xp.getActivityName().contains(transactionRemark)) {
                                alreadyProcessed = true;
                                break;
                            }
                        }
                        
                        if (alreadyProcessed) {
                            log.info("Rule Applied       : Skipped");
                            log.info("Reason             : Already processed");
                            log.info("==================================== END ====================================");
                            rewarded++;
                            continue;
                        }
                        
                        int awardXp = 0;
                        String ruleApplied = "";
                        boolean executeXp = false;
                        
                        if (Boolean.TRUE.equals(engineActivity.getAwardEnabled())) {
                            awardXp = perfectReward > 0 ? perfectReward : (engineActivity.getAwardXp() != null ? engineActivity.getAwardXp() : 0);
                            ruleApplied = "Perfect Week Reward";
                            executeXp = awardXp > 0;
                        } else {
                            ruleApplied = "Perfect Week (Reward Disabled)";
                        }
                        
                        log.info("========================================");
                        log.info("ATTENDANCE ENGINE");
                        log.info("========================================");
                        log.info("Student           : {}", student.getRegNo());
                        log.info("Attendance Status : {}%", String.format("%.1f", attendancePct));
                        log.info("Boundary          : WEEKLY");
                        log.info("Rule Selected     : {}", ruleApplied);
                        log.info("Configured XP     : {}", (executeXp ? awardXp : "0"));
                        log.info("Applied XP        : {}", (executeXp ? awardXp : 0));
                        log.info("Old Total XP      : {}", student.getTotalXp());

                        if (executeXp) {
                            try {
                                Student savedStudent = xpEngineService.awardAttendanceXpOnly(student, engineActivity, awardXp, transactionRemark);
                                log.info("New Total XP      : {}", savedStudent.getTotalXp());
                                log.info("Transaction Saved : YES");
                            } catch (Exception e) {
                                log.error("XP Execution Error : {}", e.getMessage(), e);
                                log.info("Transaction Saved : ERROR");
                            }
                        } else {
                            log.info("New Total XP      : {}", student.getTotalXp());
                            log.info("Transaction Saved : NO (No reward applied)");
                        }
                        log.info("========================================");
                        
                        rewarded++;
                    } else {
                        log.info("Rule Applied       : Not Eligible");
                        log.info("==================================== END ====================================");
                        notEligible++;
                        continue;
                    }
                    
                    log.info("==================================== END ====================================");
                } catch (Exception e) {
                    log.error("Error processing student {}: {}", student.getId(), e.getMessage());
                    errors++;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Weekly Engine Finished");
        log.info("Rewarded       : {}", rewarded);
        log.info("Not Eligible   : {}", notEligible);
        log.info("Errors         : {}", errors);
        log.info("====================================================");

        updateEngineStatus(academicYear, "DONE", LocalDateTime.now());
        return buildResult("SUCCESS", "Weekly engine completed successfully.",
                allStudents.size(), rewarded, notEligible, errors, elapsed);
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setWeeklyEngineStatus(status);
            if (runTime != null) settings.setLastWeeklyRun(runTime);
            settingsRepository.save(settings);
        });
    }

    private byte resolveYearNo(AcademicYear academicYear) {
        return switch (academicYear) {
            case FIRST_YEAR -> (byte) 1;
            case SECOND_YEAR -> (byte) 2;
            case THIRD_YEAR -> (byte) 3;
            case FOURTH_YEAR -> (byte) 4;
            default -> (byte) 1;
        };
    }

    private Map<String, Object> buildResult(String status, String message, int total, int rewarded, int notEligible, int errors, long elapsedMs) {
        return Map.of(
            "status", status,
            "message", message,
            "totalStudents", total,
            "rewarded", rewarded,
            "notEligible", notEligible,
            "errors", errors,
            "executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0)
        );
    }
}
