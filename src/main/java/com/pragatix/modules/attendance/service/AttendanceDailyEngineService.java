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
 * AttendanceDailyEngineService - executes the Daily Attendance Engine for a
 * given Academic Year.
 *
 * Uses EngineClockService to respect Production vs Test Mode.
 * Does NOT modify XP or mark attendance - only reads and logs.
 * XP transaction creation will be added in the XP Engine step.
 */
@Service
public class AttendanceDailyEngineService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDailyEngineService.class);

    @Autowired
    private EngineClockService clockService;
    @Autowired
    private AcademicCalendarResolver calendarResolver;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AttendanceSettingsRepository settingsRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private YearRepository yearRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ActivityStageRepository activityStageRepository;
    @Autowired
    private ActivityAssignmentRepository activityAssignmentRepository;
    @Autowired
    private XpEngineService xpEngineService;
    @Autowired
    private StudentActivityXpRepository studentActivityXpRepository;
    @Autowired
    private XpTransactionRepository xpTransactionRepository;
    @Autowired
    private ActivityStageMappingRepository activityStageMappingRepository;
    @Autowired
    private AcademicWeekRepository academicWeekRepository;
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

        log.info("========================================");
        log.info("DAILY ATTENDANCE ENGINE");
        log.info("Execution Date : {}", engineDate);
        log.info("========================================");

        // Holiday Check
        boolean isHoliday = calendarResolver.isHoliday(engineDate, academicYear);
        boolean isAlternate = calendarResolver.isAlternateWorkingDay(engineDate, academicYear);

        if (isHoliday) {
            updateEngineStatus(academicYear, "SKIPPED (HOLIDAY)", null);
            return buildResult("SKIPPED", "Holiday detected. Daily Engine skipped.", 0, 0, 0, 0,
                    System.currentTimeMillis() - startTime);
        }

        // Resolve yearId from Academic Year enum
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            updateEngineStatus(academicYear, "ERROR", null);
            return buildResult("ERROR", "Could not resolve Year entity for " + academicYear, 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime);
        }

        // Load all students for this year (no dept/section filter at engine level)
        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        List<ActivityStage> stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(academicYear);
        
        int processed = 0;
        int successful = 0;
        int skipped = 0;
        int errors = 0;
        
        int penaltyStudentsCount = 0;
        int partialPenaltiesCount = 0;
        int fullPenaltiesCount = 0;
        int totalXpDeducted = 0;

        try {

            for (ActivityStage stage : stages) {
                List<ActivityStageMapping> mappings = activityStageMappingRepository.findByStageId(stage.getId());

                Activity engineActivity = null;

                for (ActivityStageMapping mapping : mappings) {
                    Activity act = mapping.getActivity();

                    boolean included = true;
                    if (!Boolean.TRUE.equals(act.getAttendanceEngineEnabled())) {
                        included = false;
                    } else if (!"ACTIVE".equals(act.getStatus())) {
                        included = false;
                    } else if (!academicYear.equals(act.getAcademicYear())) {
                        included = false;
                    }

                    if (included && engineActivity == null) {
                        engineActivity = act;
                    }
                }

                if (engineActivity == null) {
                    continue;
                }

                if (!Boolean.TRUE.equals(engineActivity.getAttendanceEngineEnabled())) {
                    continue;
                }

                String rule = engineActivity.getAttendanceRule();
                if (!"DAILY".equals(rule) && !"BOTH".equals(rule)) {
                    continue;
                }

                List<ActivityAssignment> assignments = activityAssignmentRepository
                        .findByActivityId(engineActivity.getId());
                if (assignments.isEmpty()) {
                    continue;
                }

                AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
                if (settings == null) {
                    log.info("Attendance Settings not found for year. Aborting.");
                    return buildResult("ERROR", "Settings not found", 0, 0, 0, 0, 0);
                }
                int partialPenalty = settings.getPartialDayPenalty() != null ? settings.getPartialDayPenalty() : 0;
                int fullPenalty = settings.getFullDayPenalty() != null ? settings.getFullDayPenalty() : 0;

                AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate)
                        .orElse(null);

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

                int weekStartFullPenalty = settings.getWeekStartFullPenalty() != null
                        ? settings.getWeekStartFullPenalty()
                        : 0;
                int weekStartPartialPenalty = settings.getWeekStartPartialPenalty() != null
                        ? settings.getWeekStartPartialPenalty()
                        : 0;
                int weekEndFullPenalty = settings.getWeekEndFullPenalty() != null ? settings.getWeekEndFullPenalty()
                        : 0;
                int weekEndPartialPenalty = settings.getWeekEndPartialPenalty() != null
                        ? settings.getWeekEndPartialPenalty()
                        : 0;

                for (Student student : allStudents) {
                    if (student.getStage() != stage.getDisplayOrder())
                        continue;

                    boolean matchesAssignment = false;
                    for (ActivityAssignment aa : assignments) {
                        if (aa.getAssignmentScope() == AssignmentScope.GLOBAL) {
                            matchesAssignment = true;
                            break;
                        } else if (aa.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
                            if (student.getDepartment() != null
                                    && student.getDepartment().getId().equals(aa.getDepartment().getId())) {
                                matchesAssignment = true;
                                break;
                            }
                        } else if (aa.getAssignmentScope() == AssignmentScope.SECTION) {
                            if (student.getSection() != null
                                    && student.getSection().getId().equals(aa.getSection().getId())) {
                                matchesAssignment = true;
                                break;
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
                        long totalMarked = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(),
                                engineDate);

                        if (totalMarked == 0) {
                            skipped++;
                            continue;
                        }

                        String attendanceStatus;
                        if (absentCount == 0) {
                            attendanceStatus = "Perfect Day";
                        } else if (presentCount > 0) {
                            attendanceStatus = "Partial Absent";
                        } else {
                            attendanceStatus = "Full Day Absent";
                        }

                        String transactionRemark = "Attendance Date: " + engineDate;
                        boolean alreadyProcessed = false;
                        List<com.pragatix.entity.XpTransaction> existingXp = xpTransactionRepository
                                .findByStudentIdAndActivityId(student.getId(), engineActivity.getId());
                        for (com.pragatix.entity.XpTransaction xp : existingXp) {
                            if (xp.getActivityName() != null && xp.getActivityName().contains(transactionRemark)) {
                                alreadyProcessed = true;
                                break;
                            }
                        }

                        if (alreadyProcessed) {
                            skipped++;
                            processed++;
                            continue;
                        }

                        // todayType is already calculated above

                        String attendanceStr = (absentCount == 0) ? "PERFECT"
                                : (presentCount > 0 ? "PARTIAL_ABSENT" : "FULL_ABSENT");

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

                        // The XP Engine (awardAttendanceXpOnly) adds appliedXp directly, so it must be
                        // negative.
                        int appliedXp = executeXp ? -Math.abs(finalPenaltyXp) : 0;
                        int xpBefore = student.getTotalXp();

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

                                Student savedStudent = xpEngineService.awardXp(student, engineActivity, null, null,
                                        appliedXp, transactionRemark, req);
                                
                                log.info("Student:");
                                log.info(student.getRegNo());
                                log.info("");
                                log.info("Attendance:");
                                log.info(attendanceStatus);
                                log.info("");
                                log.info("Penalty:");
                                log.info("{} XP", appliedXp);
                                log.info("");
                                log.info("----------------------------------------");
                                log.info("");
                                
                                penaltyStudentsCount++;
                                if (presentCount > 0) {
                                    partialPenaltiesCount++;
                                } else {
                                    fullPenaltiesCount++;
                                }
                                totalXpDeducted += appliedXp;
                                
                            } catch (Exception e) {
                                log.error("XP Execution Error : {}", e.getMessage(), e);
                            }
                        }

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
            log.info("SUMMARY");
            log.info("");
            log.info("Students Processed : {}", allStudents.size());
            log.info("");
            log.info("Penalty Students : {}", penaltyStudentsCount);
            log.info("");
            log.info("Partial Penalties : {}", partialPenaltiesCount);
            log.info("");
            log.info("Full Penalties : {}", fullPenaltiesCount);
            log.info("");
            log.info("Total XP Deducted : {}", totalXpDeducted);
            log.info("");
            log.info("Execution Time : {} seconds", String.format("%.1f", elapsed / 1000.0));
            log.info("");
            log.info("DAILY ENGINE COMPLETED");

            if (processed == 0 && skipped == 0) {
                updateEngineStatus(academicYear, "NO DATA PROCESSED", LocalDateTime.now());
                return buildResult("SUCCESS", "Daily engine completed but no students were processed.",
                        0, 0, 0, errors, elapsed);
            } else {
                updateEngineStatus(academicYear, "DONE", LocalDateTime.now());
                return buildResult("SUCCESS", "Daily engine completed successfully.",
                        processed + skipped, successful, skipped, errors, elapsed);
            }

        } catch (Exception e) {
            log.error("AttendanceService failed");
            log.error("Exception: {}", e.getMessage(), e);
            log.info("lastDailyRun NOT updated");
            updateEngineStatus(academicYear, "FAILED", null);
            return buildResult("ERROR", "Engine execution failed: " + e.getMessage(), 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime);
        }
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setDailyEngineStatus(status);
            if (runTime != null)
                settings.setLastDailyRun(runTime);
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

    private Map<String, Object> buildResult(String status, String message, int total, int successful, int skipped,
            int errors, long elapsedMs) {
        return Map.of(
                "status", status,
                "message", message,
                "totalStudents", total,
                "successful", successful,
                "skipped", skipped,
                "errors", errors,
                "executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0));
    }
}
