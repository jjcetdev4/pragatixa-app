package com.pragatix.modules.attendance.service;

import com.pragatix.entity.Attendance;
import com.pragatix.entity.AttendanceSettings;
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
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AttendanceDailyEngineService — executes the Daily Attendance Engine for a given Academic Year.
 *
 * Uses EngineClockService to respect Production vs Test Mode.
 * Does NOT modify XP or mark attendance — only reads and logs.
 * XP transaction creation will be added in the XP Engine step.
 */
@Service
public class AttendanceDailyEngineService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDailyEngineService.class);

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
    private final ApplicationContext applicationContext;

    public AttendanceDailyEngineService(StudentRepository studentRepository, 
                                        AttendanceRepository attendanceRepository,
                                        ActivityStageMappingRepository activityStageMappingRepository,
                                        ActivityRepository activityRepository,
                                        ActivityAssignmentRepository activityAssignmentRepository,
                                        ActivityStageRepository activityStageRepository,
                                        StudentActivityXpRepository studentActivityXpRepository,
                                        XpTransactionRepository xpTransactionRepository,
                                        AttendanceSettingsRepository settingsRepository,
                                        XpEngineService xpEngineService,
                                        ApplicationContext applicationContext,
                                        AcademicWeekRepository academicWeekRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityStageRepository = activityStageRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.settingsRepository = settingsRepository;
        this.xpEngineService = xpEngineService;
        this.applicationContext = applicationContext;
        this.academicWeekRepository = academicWeekRepository;
    }

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear) {
        long startTime = System.currentTimeMillis();
        LocalDate engineDate = clockService.getEffectiveDate(academicYear);
        boolean testMode = clockService.isTestMode(academicYear);

        log.info("====================================================");
        log.info("ATTENDANCE DAILY ENGINE STARTED");
        log.info("====================================================");
        log.info("Academic Year : {}", academicYear);
        log.info("Engine Mode   : {}", testMode ? "TEST" : "PRODUCTION");
        log.info("Engine Date   : {}", engineDate);
        log.info("Engine Time   : {}", clockService.getEffectiveTime(academicYear));

        // Holiday Check
        boolean isHoliday = calendarResolver.isHoliday(engineDate, academicYear);
        boolean isAlternate = calendarResolver.isAlternateWorkingDay(engineDate, academicYear);
        log.info("Holiday        : {}", isHoliday);
        log.info("Alternate Day  : {}", isAlternate);

        if (isHoliday) {
            log.info("Holiday detected — Skipping Daily Engine");
            log.info("====================================================");
            updateEngineStatus(academicYear, "SKIPPED (HOLIDAY)", null);
            return buildResult("SKIPPED", "Holiday detected. Daily Engine skipped.", 0, 0, 0, 0, System.currentTimeMillis() - startTime);
        }

        // Resolve yearId from Academic Year enum
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            log.warn("Could not resolve Year entity for {}", academicYear);
            updateEngineStatus(academicYear, "ERROR", null);
            return buildResult("ERROR", "Could not resolve Year entity for " + academicYear, 0, 0, 0, 1, System.currentTimeMillis() - startTime);
        }

        // Load all students for this year (no dept/section filter at engine level)
        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        log.info("Students Found : {}", allStudents.size());
        
        List<ActivityStage> stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(academicYear);
        int processed = 0;
        int successful = 0;
        int skipped = 0;
        int errors = 0;

        for (ActivityStage stage : stages) {
            log.info("====================================================");
            log.info("DAILY ENGINE START");
            log.info("====================================================");
            log.info("Academic Year : {}", academicYear);
            log.info("Current Stage : {}", stage.getName());


            // --- TEMPORARY FORENSIC LOGS (Safe to remove later) ---
            log.info("==================================================");
            log.info("ATTENDANCE ENGINE RESOLUTION (USING MAPPINGS)");
            log.info("Current Stage           : {} (ID: {})", stage.getName(), stage.getId());
            
            List<ActivityStageMapping> mappings = activityStageMappingRepository.findByStageId(stage.getId());
        
        log.info("================ FORENSIC TRACE ==================");
        if (!mappings.isEmpty()) {
            Activity act = mappings.get(0).getActivity();
            log.info("engineActivity.getClass().getName() : {}", act.getClass().getName());
            log.info("engineActivity originates from ActivityStageMapping.getActivity()");
            log.info("Properties on Java Object:");
            log.info("awardEnabled: {}", act.getAwardEnabled());
            log.info("penaltyEnabled: {}", act.getPenaltyEnabled());
            log.info("awardXp: {}", act.getAwardXp());
            log.info("penaltyXp: {}", act.getPenaltyXp());
            
            // Raw SQL dump
            try {
                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = applicationContext.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                java.util.List<java.util.Map<String, Object>> activitiesRows = jdbcTemplate.queryForList(
                    "SELECT award_enabled, penalty_enabled, award_xp, penalty_xp FROM activities WHERE id = ?", act.getId());
                log.info("Raw SQL activities table for ID {}: {}", act.getId(), activitiesRows);
                
                java.util.List<java.util.Map<String, Object>> mappingsRows = jdbcTemplate.queryForList(
                    "SELECT award_enabled, penalty_enabled, award_xp, penalty_xp FROM activity_stage_mappings WHERE activity_id = ? AND stage_id = ?", act.getId(), stage.getId());
                log.info("Raw SQL activity_stage_mappings table for Activity {} Stage {}: {}", act.getId(), stage.getId(), mappingsRows);
            } catch (Exception e) {
                log.error("Failed to execute native SQL: {}", e.getMessage());
            }
        }
        log.info("==================================================");

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
            if (!"DAILY".equals(rule) && !"BOTH".equals(rule)) {
                log.info("Attendance Engine skipped.");
                log.info("Reason: Activity Rule is {}, skipping daily XP penalty.", rule);
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
            
            System.out.println("Students from DB = " + allStudents.size());
            System.out.println("Current Engine Date = " + engineDate);

            AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
            if (settings == null) {
                log.info("Attendance Settings not found for year. Aborting.");
                return buildResult("ERROR", "Settings not found", 0, 0, 0, 0, 0);
            }
            int partialPenalty = settings.getPartialDayPenalty() != null ? settings.getPartialDayPenalty() : 0;
            int fullPenalty = settings.getFullDayPenalty() != null ? settings.getFullDayPenalty() : 0;
            
            AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate).orElse(null);
            
            if (activeWeek == null) {
                log.info("No active Academic Week configured. Aborting.");
                return buildResult("ERROR", "No active Academic Week configured", 0, 0, 0, 0, 0);
            }
            
            java.time.LocalDate startDate = activeWeek.getStartDate();
            java.time.LocalDate endDate = activeWeek.getEndDate();
            
            String todayType = "NORMAL";
            if (engineDate.isEqual(startDate)) {
                todayType = "WEEK_START";
            } else if (engineDate.isEqual(endDate)) {
                todayType = "WEEK_END";
            }

            log.info("==================================");
            log.info("ATTENDANCE DATE CONFIGURATION");
            log.info("==================================");
            log.info("Academic Week         : {}", activeWeek.getAcademicMonth().getAcademicYearEnum());
            log.info("Week Number           : {}", activeWeek.getWeekNumber());
            log.info("Week Start Date       : {}", startDate);
            log.info("Week End Date         : {}", endDate);
            log.info("Current Engine Date   : {}", engineDate);
            log.info("Boundary Detected     : {}", todayType);
            log.info("==================================");
            
            int weekStartFullPenalty = settings.getWeekStartFullPenalty() != null ? settings.getWeekStartFullPenalty() : 0;
            int weekStartPartialPenalty = settings.getWeekStartPartialPenalty() != null ? settings.getWeekStartPartialPenalty() : 0;
            int weekEndFullPenalty = settings.getWeekEndFullPenalty() != null ? settings.getWeekEndFullPenalty() : 0;
            int weekEndPartialPenalty = settings.getWeekEndPartialPenalty() != null ? settings.getWeekEndPartialPenalty() : 0;

            log.info("================ FORENSIC TRACE: SETTINGS RETRIEVAL ================");
            log.info("Week Start Full Penalty    = {}", weekStartFullPenalty);
            log.info("Week Start Partial Penalty = {}", weekStartPartialPenalty);
            log.info("Week End Full Penalty      = {}", weekEndFullPenalty);
            log.info("Week End Partial Penalty   = {}", weekEndPartialPenalty);
            log.info("Normal Full Day Penalty    = {}", fullPenalty);
            log.info("Normal Partial Day Penalty = {}", partialPenalty);
            log.info("====================================================================");
            
            for (Student student : allStudents) {
                if (student.getStage() != stage.getDisplayOrder()) continue;
                
                System.out.println("Processing Student = " + student.getRegNo());

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
                    continue; // Skip student not in assignment scope
                }

                try {
                    long presentCount = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                            student.getId(), engineDate, Attendance.AttendanceStatus.PRESENT);
                    long absentCount = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                            student.getId(), engineDate, Attendance.AttendanceStatus.ABSENT);
                    long totalMarked = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(), engineDate);

                    System.out.println("Attendance Found = " + (totalMarked > 0));

                    if (totalMarked == 0) {
                        skipped++;
                        continue;
                    }

                    String attendanceStatus;
                    String xpAction;
                    if (absentCount == 0) {
                        attendanceStatus = "Perfect Day";
                        xpAction = "No penalty";
                    } else if (presentCount > 0) {
                        attendanceStatus = "Partial Absent";
                        xpAction = "Partial Day Penalty (pending XP engine)";
                    } else {
                        attendanceStatus = "Full Day Absent";
                        xpAction = "Full Day Penalty (pending XP engine)";
                    }

                    log.info("==================================== ATTENDANCE XP EXECUTION ====================================");
                    log.info("Student ID         : {}", student.getId());
                    log.info("Student Name       : {}", student.getUser() != null ? student.getUser().getFullName() : student.getRegNo());
                    log.info("Activity ID        : {}", engineActivity.getId());
                    log.info("Activity Name      : {}", engineActivity.getName());
                    log.info("Attendance Status  : {}", attendanceStatus);
                    
                    String transactionRemark = "Attendance Date: " + engineDate;
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
                        skipped++;
                        processed++;
                        continue;
                    }
                    
                    // todayType is already calculated above
                    
                    String attendanceStr = (absentCount == 0) ? "PERFECT" : (presentCount > 0 ? "PARTIAL_ABSENT" : "FULL_ABSENT");
                    
                    int finalPenaltyXp = 0;
                    String penaltySource = "None";
                    boolean executeXp = false;
                    
                    if (absentCount == 0) { // Perfect
                        penaltySource = "None (Perfect Attendance)";
                    } else if (presentCount > 0) { // Partial Absent
                        if (Boolean.TRUE.equals(engineActivity.getPenaltyEnabled())) {
                            if ("WEEK_START".equals(todayType)) {
                                finalPenaltyXp = weekStartPartialPenalty;
                                penaltySource = "weekStartPartialPenalty";
                            } else if ("WEEK_END".equals(todayType)) {
                                finalPenaltyXp = weekEndPartialPenalty;
                                penaltySource = "weekEndPartialPenalty";
                            } else {
                                finalPenaltyXp = partialPenalty;
                                penaltySource = "partial_day_penalty";
                            }
                        } else {
                            penaltySource = "Penalty Disabled";
                        }
                    } else { // Full Absent
                        if (Boolean.TRUE.equals(engineActivity.getPenaltyEnabled())) {
                            if ("WEEK_START".equals(todayType)) {
                                finalPenaltyXp = weekStartFullPenalty;
                                penaltySource = "weekStartFullPenalty";
                            } else if ("WEEK_END".equals(todayType)) {
                                finalPenaltyXp = weekEndFullPenalty;
                                penaltySource = "weekEndFullPenalty";
                            } else {
                                finalPenaltyXp = fullPenalty;
                                penaltySource = "full_day_penalty";
                            }
                        } else {
                            penaltySource = "Penalty Disabled";
                        }
                    }
                    
                    // We check != 0 because penalties are negative numbers (e.g., -40)
                    executeXp = finalPenaltyXp != 0;
                    
                    // The XP Engine (awardAttendanceXpOnly) adds appliedXp directly, so it must be negative.
                    int appliedXp = executeXp ? -Math.abs(finalPenaltyXp) : 0;
                    int xpBefore = student.getTotalXp();
                    
                    log.info("================ FORENSIC TRACE: XP EXECUTION ================");
                    log.info("Boundary                      : {}", todayType);
                    log.info("Selected Rule                 : {}", penaltySource);
                    log.info("Penalty From Settings         : {}", finalPenaltyXp);
                    log.info("Final Penalty Sent To XP Engine : {}", appliedXp);
                    log.info("XP Engine Received            : {}", appliedXp);
                    log.info("XP Applied                    : {}", executeXp ? appliedXp : 0);
                    log.info("==============================================================");
                    
                    log.info("========================================");
                    log.info("ATTENDANCE ENGINE");
                    log.info("========================================");
                    log.info("Student           : {}", student.getRegNo());
                    log.info("Attendance Status : {}", attendanceStr);
                    log.info("Boundary          : {}", todayType);
                    log.info("Rule Selected     : {}", penaltySource);
                    log.info("Configured XP     : {}", (executeXp ? finalPenaltyXp : "0"));
                    log.info("Applied XP        : {}", appliedXp);
                    log.info("Old Total XP      : {}", xpBefore);
                    
                    if (executeXp) {
                        try {
                            com.pragatix.modules.attendance.dto.AttendanceXpExecutionRequest req = new com.pragatix.modules.attendance.dto.AttendanceXpExecutionRequest();
                            req.setStudentId(student.getId());
                            req.setActivityId(engineActivity.getId());
                            req.setAttendanceRule(penaltySource);
                            req.setCalculatedXp(appliedXp);
                            req.setIsPenalty(appliedXp < 0);
                            req.setAttendanceDate(engineDate);
                            req.setWeekStartDate(startDate);
                            req.setWeekEndDate(endDate);
                            req.setReason("Attendance Daily Rule: " + penaltySource);
                            req.setRemarks(transactionRemark);

                            Student savedStudent = xpEngineService.awardXp(student, engineActivity, null, null, appliedXp, transactionRemark, req);
                            log.info("New Total XP      : {}", savedStudent.getTotalXp());
                            log.info("Transaction Saved : YES");
                        } catch (Exception e) {
                            log.error("XP Execution Error : {}", e.getMessage(), e);
                            log.info("Transaction Saved : ERROR");
                        }
                    } else {
                        log.info("New Total XP      : {}", xpBefore);
                        log.info("Transaction Saved : NO (No penalty applied)");
                    }
                    log.info("========================================");

                    successful++;
                    processed++;
                } catch (Exception e) {
                    log.error("Error processing student {}: {}", student.getId(), e.getMessage());
                    errors++;
                    processed++;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("====================================================");
        log.info("DAILY ENGINE FINISHED");
        log.info("Processed     : {}", processed + skipped);
        log.info("Successful    : {}", successful);
        log.info("Skipped       : {}", skipped);
        log.info("Errors        : {}", errors);
        log.info("Execution Time: {} seconds", String.format("%.1f", elapsed / 1000.0));
        log.info("====================================================");

        updateEngineStatus(academicYear, "DONE", LocalDateTime.now());
        return buildResult("SUCCESS", "Daily engine completed successfully.",
                processed + skipped, successful, skipped, errors, elapsed);
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setDailyEngineStatus(status);
            if (runTime != null) settings.setLastDailyRun(runTime);
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

    private Map<String, Object> buildResult(String status, String message, int total, int successful, int skipped, int errors, long elapsedMs) {
        return Map.of(
            "status", status,
            "message", message,
            "totalStudents", total,
            "successful", successful,
            "skipped", skipped,
            "errors", errors,
            "executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0)
        );
    }
}
