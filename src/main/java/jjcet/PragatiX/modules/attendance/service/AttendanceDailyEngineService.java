package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.entity.Attendance;
import jjcet.PragatiX.entity.AttendanceEngineExecution;
import jjcet.PragatiX.entity.AttendanceSettings;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver;
import jjcet.PragatiX.modules.attendance.repository.AttendanceEngineExecutionRepository;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import jjcet.PragatiX.modules.attendancesettings.repository.AttendanceSettingsRepository;
import jjcet.PragatiX.modules.attendancesettings.service.EngineClockService;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.YearRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.AcademicWeek;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicWeekRepository;
import jjcet.PragatiX.modules.student.service.XpEngineService;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.repository.XpTransactionRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageMappingRepository;
import jjcet.PragatiX.entity.ActivityStageMapping;
import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.ActivityStage;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.entity.AssignmentScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AttendanceDailyEngineService - executes the Daily Attendance Engine for a given Academic Year.
 * 
 * Unified service for both Manual Trigger and Automatic Cron Scheduler.
 * Features:
 * - Full-day attendance streak increment (idempotent per student + date)
 * - Absence streak reset/handling
 * - Day-specific penalty calculations (Normal, Week-Start, Week-End, Partial vs Full)
 * - Execution History tracking (AttendanceEngineExecution)
 * - Duplicate execution prevention (idempotent period matching)
 * - Concurrency protection
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
    @Autowired
    private AttendanceStreakService attendanceStreakService;
    @Autowired
    private AttendanceEngineExecutionRepository executionRepository;

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
            AcademicWeekRepository academicWeekRepository,
            AttendanceStreakService attendanceStreakService,
            AttendanceEngineExecutionRepository executionRepository) {
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
        this.academicWeekRepository = academicWeekRepository;
        this.attendanceStreakService = attendanceStreakService;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear) {
        return execute(academicYear, null, "AUTOMATIC", "SYSTEM");
    }

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear, LocalDate targetDate, String executionType, String triggeredBy) {
        long startTime = System.currentTimeMillis();
        LocalDate engineDate = targetDate != null ? targetDate : clockService.getEffectiveDate(academicYear);
        if (executionType == null || executionType.trim().isEmpty()) {
            executionType = "MANUAL";
        }
        if (triggeredBy == null || triggeredBy.trim().isEmpty()) {
            triggeredBy = "SYSTEM";
        }

        log.info("========================================");
        log.info("DAILY ATTENDANCE ENGINE");
        log.info("Academic Year  : {}", academicYear);
        log.info("Execution Date : {}", engineDate);
        log.info("Execution Type : {}", executionType);
        log.info("Triggered By   : {}", triggeredBy);
        log.info("========================================");

        // 1. Holiday Check
        boolean isHoliday = calendarResolver.isHoliday(engineDate, academicYear);
        if (isHoliday) {
            updateEngineStatus(academicYear, "SKIPPED (HOLIDAY)", executionType, "SUCCESS", LocalDateTime.now());
            return buildResult("SKIPPED", "Holiday detected on " + engineDate + ". Daily Engine skipped.", 0, 0, 0, 0,
                    0, 0, 0, System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
        }

        // 2. Duplicate Execution Check (Idempotency)
        Optional<AttendanceEngineExecution> successfulRun = executionRepository
                .findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndAndStatus(
                        academicYear, "DAILY", engineDate, engineDate, "SUCCESS");

        if (successfulRun.isPresent()) {
            log.info("Daily Engine already successfully processed for {}. Skipping duplicate execution.", engineDate);
            return buildResult("SKIPPED", "Daily Engine for " + engineDate + " has already been successfully processed.",
                    successfulRun.get().getProcessedCount(), successfulRun.get().getPresentCount(),
                    successfulRun.get().getAbsentCount(), successfulRun.get().getPenaltiesApplied(),
                    successfulRun.get().getStreaksUpdated(), successfulRun.get().getSkippedCount(),
                    0, System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
        }

        // 3. Concurrency Protection (Check if currently running)
        Optional<AttendanceEngineExecution> runningRun = executionRepository
                .findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndAndStatus(
                        academicYear, "DAILY", engineDate, engineDate, "RUNNING");
        if (runningRun.isPresent() && runningRun.get().getStartedAt() != null
                && runningRun.get().getStartedAt().isAfter(LocalDateTime.now().minusMinutes(10))) {
            log.warn("Daily Engine is already RUNNING for {}. Concurrency lock applied.", engineDate);
            return buildResult("RUNNING", "Daily Engine is currently running for " + engineDate,
                    0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
        }

        // 4. Create Execution Tracking Record
        AttendanceEngineExecution execution = new AttendanceEngineExecution();
        execution.setAcademicYear(academicYear);
        execution.setEngineType("DAILY");
        execution.setPeriodStart(engineDate);
        execution.setPeriodEnd(engineDate);
        execution.setExecutionType(executionType);
        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);
        execution = executionRepository.save(execution);

        updateEngineStatus(academicYear, "RUNNING", executionType, "RUNNING", LocalDateTime.now());

        // 5. Resolve yearId from Academic Year enum
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            execution.setStatus("FAILED");
            execution.setErrorMessage("Could not resolve Year entity for " + academicYear);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            executionRepository.save(execution);
            updateEngineStatus(academicYear, "ERROR", executionType, "FAILED", null);
            return buildResult("ERROR", "Could not resolve Year entity for " + academicYear, 0, 0, 0, 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
        }

        // Load all students for this year
        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        List<ActivityStage> stages = activityStageRepository.findByAcademicYearAndDeletedFalseOrderByDisplayOrderAsc(academicYear);

        int processed = 0;
        int successful = 0;
        int skipped = 0;
        int errors = 0;

        int presentStudentsCount = 0;
        int absentStudentsCount = 0;
        int penaltyStudentsCount = 0;
        int partialPenaltiesCount = 0;
        int fullPenaltiesCount = 0;
        int streaksUpdatedCount = 0;
        int totalXpDeducted = 0;

        try {
            AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
            if (settings == null) {
                execution.setStatus("FAILED");
                execution.setErrorMessage("Attendance Settings not found for year " + academicYear);
                execution.setCompletedAt(LocalDateTime.now());
                executionRepository.save(execution);
                updateEngineStatus(academicYear, "ERROR", executionType, "FAILED", null);
                return buildResult("ERROR", "Settings not found", 0, 0, 0, 0, 0, 0, 1,
                        System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
            }

            int partialPenalty = settings.getPartialDayPenalty() != null ? settings.getPartialDayPenalty() : 0;
            int fullPenalty = settings.getFullDayPenalty() != null ? settings.getFullDayPenalty() : 0;

            AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate).orElse(null);
            boolean executePenaltyConfigured = (activeWeek != null);
            LocalDate startDate = null;
            LocalDate endDate = null;
            String todayType = "NORMAL";

            if (executePenaltyConfigured) {
                startDate = activeWeek.getStartDate();
                endDate = activeWeek.getEndDate();
                if (engineDate.isEqual(startDate)) {
                    todayType = "WEEK_START";
                } else if (engineDate.isEqual(endDate)) {
                    todayType = "WEEK_END";
                }
            } else {
                log.info("No active Academic Week configured for {}. Streak will be processed; penalties will use base settings.", engineDate);
            }

            int weekStartFullPenalty = settings.getWeekStartFullPenalty() != null ? settings.getWeekStartFullPenalty() : fullPenalty;
            int weekStartPartialPenalty = settings.getWeekStartPartialPenalty() != null ? settings.getWeekStartPartialPenalty() : partialPenalty;
            int weekEndFullPenalty = settings.getWeekEndFullPenalty() != null ? settings.getWeekEndFullPenalty() : fullPenalty;
            int weekEndPartialPenalty = settings.getWeekEndPartialPenalty() != null ? settings.getWeekEndPartialPenalty() : partialPenalty;

            for (ActivityStage stage : stages) {
                List<ActivityStageMapping> mappings = activityStageMappingRepository.findByStageId(stage.getId());
                Activity engineActivity = null;

                for (ActivityStageMapping mapping : mappings) {
                    Activity act = mapping.getActivity();
                    if (act != null && Boolean.TRUE.equals(act.getAttendanceEngineEnabled())
                            && "ACTIVE".equals(act.getStatus())
                            && academicYear.equals(act.getAcademicYear())) {
                        engineActivity = act;
                        break;
                    }
                }

                if (engineActivity == null || !Boolean.TRUE.equals(engineActivity.getAttendanceEngineEnabled())) {
                    continue;
                }

                String rule = engineActivity.getAttendanceRule();
                if (!"DAILY".equals(rule) && !"BOTH".equals(rule)) {
                    continue;
                }

                List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(engineActivity.getId());
                if (assignments.isEmpty()) {
                    continue;
                }

                for (Student student : allStudents) {
                    if (student.getStage() != stage.getDisplayOrder()) {
                        continue;
                    }

                    boolean matchesAssignment = false;
                    for (ActivityAssignment aa : assignments) {
                        if (aa.getAssignmentScope() == AssignmentScope.GLOBAL) {
                            matchesAssignment = true;
                            break;
                        } else if (aa.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
                            if (student.getDepartment() != null && student.getDepartment().getId().equals(aa.getDepartment().getId())) {
                                matchesAssignment = true;
                                break;
                            }
                        } else if (aa.getAssignmentScope() == AssignmentScope.SECTION) {
                            if (student.getSection() != null && student.getSection().getId().equals(aa.getSection().getId())) {
                                matchesAssignment = true;
                                break;
                            }
                        }
                    }

                    if (!matchesAssignment) {
                        continue;
                    }

                    try {
                        long presentCount = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                student.getId(), engineDate, Attendance.AttendanceStatus.PRESENT);
                        long absentCount = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                student.getId(), engineDate, Attendance.AttendanceStatus.ABSENT);
                        long totalMarked = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(), engineDate);

                        // If no attendance marked at all, skip student
                        if (totalMarked == 0) {
                            skipped++;
                            continue;
                        }

                        boolean isFullDayPresent = (absentCount == 0 && totalMarked > 0);

                        if (isFullDayPresent) {
                            // Student is FULL-DAY PRESENT
                            presentStudentsCount++;
                            try {
                                attendanceStreakService.incrementAttendanceStreak(student, engineDate);
                                streaksUpdatedCount++;
                            } catch (Exception e) {
                                log.error("Streak increment error for student {}: {}", student.getRegNo(), e.getMessage());
                            }
                        } else {
                            // Student is ABSENT or PARTIAL ABSENT
                            absentStudentsCount++;
                            try {
                                attendanceStreakService.handleAbsenceStreak(student, engineDate);
                            } catch (Exception e) {
                                log.error("Streak absence reset error for student {}: {}", student.getRegNo(), e.getMessage());
                            }

                            // Calculate configured penalty
                            int finalPenaltyXp = 0;
                            String penaltySource = "None";

                            if (Boolean.TRUE.equals(engineActivity.getPenaltyEnabled())) {
                                if (presentCount > 0) { // Partial Absent
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
                                } else { // Full Absent
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
                                }
                            }

                            if (finalPenaltyXp != 0) {
                                String transactionRemark = "Attendance Date: " + engineDate;
                                boolean alreadyPenalized = false;
                                List<jjcet.PragatiX.entity.XpTransaction> existingXp = xpTransactionRepository
                                        .findByStudentIdAndActivityId(student.getId(), engineActivity.getId());
                                for (jjcet.PragatiX.entity.XpTransaction xp : existingXp) {
                                    if (xp.getActivityName() != null && xp.getActivityName().contains(transactionRemark)) {
                                        alreadyPenalized = true;
                                        break;
                                    }
                                }

                                if (!alreadyPenalized) {
                                    int appliedXp = -Math.abs(finalPenaltyXp);
                                    jjcet.PragatiX.modules.attendance.dto.AttendanceXpExecutionRequest req = new jjcet.PragatiX.modules.attendance.dto.AttendanceXpExecutionRequest();
                                    req.setStudentId(student.getId());
                                    req.setActivityId(engineActivity.getId());
                                    req.setAttendanceRule(penaltySource);
                                    req.setCalculatedXp(appliedXp);
                                    req.setIsPenalty(true);
                                    req.setAttendanceDate(engineDate);
                                    req.setWeekStartDate(startDate);
                                    req.setWeekEndDate(endDate);
                                    req.setReason("Attendance Daily Rule: " + penaltySource);
                                    req.setRemarks(transactionRemark);

                                    xpEngineService.awardXp(student, engineActivity, null, null, appliedXp, transactionRemark, req);

                                    penaltyStudentsCount++;
                                    if (presentCount > 0) {
                                        partialPenaltiesCount++;
                                    } else {
                                        fullPenaltiesCount++;
                                    }
                                    totalXpDeducted += appliedXp;
                                }
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
            log.info("Students Processed : {}", processed);
            log.info("Present Students   : {}", presentStudentsCount);
            log.info("Absent Students    : {}", absentStudentsCount);
            log.info("Penalties Applied  : {}", penaltyStudentsCount);
            log.info("Streaks Updated    : {}", streaksUpdatedCount);
            log.info("Total XP Deducted  : {}", totalXpDeducted);
            log.info("Execution Time     : {} ms", elapsed);
            log.info("DAILY ENGINE COMPLETED");

            // Update Execution Record
            execution.setStatus("SUCCESS");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setProcessedCount(processed);
            execution.setPresentCount(presentStudentsCount);
            execution.setAbsentCount(absentStudentsCount);
            execution.setPenaltiesApplied(penaltyStudentsCount);
            execution.setStreaksUpdated(streaksUpdatedCount);
            execution.setSkippedCount(skipped);
            execution.setFailureCount(errors);
            execution.setExecutionTimeMs(elapsed);
            executionRepository.save(execution);

            updateEngineStatus(academicYear, "SUCCESS", executionType, "SUCCESS", LocalDateTime.now());

            return buildResult("SUCCESS", "Daily Attendance Engine completed successfully.",
                    processed, presentStudentsCount, absentStudentsCount, penaltyStudentsCount,
                    streaksUpdatedCount, skipped, errors, elapsed, executionType, engineDate, engineDate);

        } catch (Exception e) {
            log.error("Daily Attendance Engine failed: {}", e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            executionRepository.save(execution);

            updateEngineStatus(academicYear, "FAILED", executionType, "FAILED", LocalDateTime.now());

            return buildResult("ERROR", "Engine execution failed: " + e.getMessage(),
                    processed, presentStudentsCount, absentStudentsCount, penaltyStudentsCount,
                    streaksUpdatedCount, skipped, errors + 1, System.currentTimeMillis() - startTime,
                    executionType, engineDate, engineDate);
        }
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, String runType, String runStatus, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setDailyEngineStatus(status);
            if (runType != null) {
                settings.setLastDailyRunType(runType);
            }
            if (runStatus != null) {
                settings.setLastDailyRunStatus(runStatus);
            }
            if (runTime != null) {
                settings.setLastDailyRun(runTime);
            }
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

    private Map<String, Object> buildResult(String status, String message, int total, int present, int absent,
            int penalties, int streaks, int skipped, int errors, long elapsedMs,
            String executionType, LocalDate periodStart, LocalDate periodEnd) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("message", message);
        result.put("processedStudents", total);
        result.put("presentStudents", present);
        result.put("absentStudents", absent);
        result.put("penaltiesApplied", penalties);
        result.put("streaksUpdated", streaks);
        result.put("skippedStudents", skipped);
        result.put("failedStudents", errors);
        result.put("executionType", executionType);
        result.put("periodStart", periodStart != null ? periodStart.toString() : null);
        result.put("periodEnd", periodEnd != null ? periodEnd.toString() : null);
        result.put("executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0));
        return result;
    }
}
