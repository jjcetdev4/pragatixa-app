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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AttendanceWeeklyEngineService - executes the Weekly Attendance Engine for a given Academic Year.
 * 
 * Unified service for both Manual Trigger and Automatic Cron Scheduler.
 * Features:
 * - Weekly full-attendance award calculation (all working days present)
 * - Strict duplicate protection per student + week
 * - Execution History tracking (AttendanceEngineExecution)
 * - Concurrency protection
 * - Independent from daily streak logic (does NOT touch streak)
 */
@Service
public class AttendanceWeeklyEngineService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceWeeklyEngineService.class);

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
    private AttendanceEngineExecutionRepository executionRepository;

    public AttendanceWeeklyEngineService(StudentRepository studentRepository,
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
        this.executionRepository = executionRepository;
    }

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear) {
        return execute(academicYear, null, null, "AUTOMATIC", "SYSTEM");
    }

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear, LocalDate customStartDate, LocalDate customEndDate, String executionType, String triggeredBy) {
        long startTime = System.currentTimeMillis();
        LocalDate engineDate = clockService.getEffectiveDate(academicYear);
        if (executionType == null || executionType.trim().isEmpty()) {
            executionType = "MANUAL";
        }
        if (triggeredBy == null || triggeredBy.trim().isEmpty()) {
            triggeredBy = "SYSTEM";
        }

        AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
        if (settings == null) {
            return buildResult("ERROR", "Settings not found for " + academicYear, 0, 0, 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
        }

        LocalDate startDate = customStartDate;
        LocalDate endDate = customEndDate;

        if (startDate == null || endDate == null) {
            AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate).orElse(null);
            if (activeWeek == null) {
                return buildResult("ERROR", "No active Academic Week configured for " + academicYear + " on date " + engineDate,
                        0, 0, 0, 0, 0, 1, System.currentTimeMillis() - startTime, executionType, engineDate, engineDate);
            }
            startDate = activeWeek.getStartDate();
            endDate = activeWeek.getEndDate();
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        String weekStr = startDate.format(formatter) + " - " + endDate.format(formatter);

        log.info("========================================");
        log.info("WEEKLY ATTENDANCE ENGINE");
        log.info("Academic Year  : {}", academicYear);
        log.info("Week Range     : {}", weekStr);
        log.info("Execution Type : {}", executionType);
        log.info("Triggered By   : {}", triggeredBy);
        log.info("========================================");

        // 1. Duplicate Execution Check (Idempotency)
        Optional<AttendanceEngineExecution> successfulRun = executionRepository
                .findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndAndStatus(
                        academicYear, "WEEKLY", startDate, endDate, "SUCCESS");

        if (successfulRun.isPresent()) {
            log.info("Weekly Engine already successfully processed for {}. Skipping duplicate execution.", weekStr);
            return buildResult("SKIPPED", "Weekly Engine for " + weekStr + " has already been successfully processed.",
                    successfulRun.get().getProcessedCount(), successfulRun.get().getPresentCount(),
                    successfulRun.get().getAbsentCount(), successfulRun.get().getPenaltiesApplied(),
                    successfulRun.get().getSkippedCount(), 0, System.currentTimeMillis() - startTime,
                    executionType, startDate, endDate);
        }

        // 2. Concurrency Protection (Check if currently running)
        Optional<AttendanceEngineExecution> runningRun = executionRepository
                .findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndAndStatus(
                        academicYear, "WEEKLY", startDate, endDate, "RUNNING");
        if (runningRun.isPresent() && runningRun.get().getStartedAt() != null
                && runningRun.get().getStartedAt().isAfter(LocalDateTime.now().minusMinutes(10))) {
            log.warn("Weekly Engine is already RUNNING for {}. Concurrency lock applied.", weekStr);
            return buildResult("RUNNING", "Weekly Engine is currently running for " + weekStr,
                    0, 0, 0, 0, 0, 0, System.currentTimeMillis() - startTime, executionType, startDate, endDate);
        }

        // 3. Create Execution Tracking Record
        AttendanceEngineExecution execution = new AttendanceEngineExecution();
        execution.setAcademicYear(academicYear);
        execution.setEngineType("WEEKLY");
        execution.setPeriodStart(startDate);
        execution.setPeriodEnd(endDate);
        execution.setExecutionType(executionType);
        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);
        execution = executionRepository.save(execution);

        updateEngineStatus(academicYear, "RUNNING", executionType, "RUNNING", LocalDateTime.now());

        // 4. Working days calculation for the week (excluding holidays)
        List<LocalDate> workingDays = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (calendarResolver.isWorkingDay(d, academicYear)) {
                workingDays.add(d);
            }
        }

        if (workingDays.isEmpty()) {
            execution.setStatus("SUCCESS");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setErrorMessage("No working days in week range " + weekStr);
            executionRepository.save(execution);
            updateEngineStatus(academicYear, "SUCCESS", executionType, "SUCCESS", LocalDateTime.now());
            return buildResult("SUCCESS", "No working days detected in the week. Engine completed.",
                    0, 0, 0, 0, 0, 0, System.currentTimeMillis() - startTime, executionType, startDate, endDate);
        }

        // 5. Resolve yearId
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            execution.setStatus("FAILED");
            execution.setErrorMessage("Could not resolve Year entity for " + academicYear);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            updateEngineStatus(academicYear, "ERROR", executionType, "FAILED", null);
            return buildResult("ERROR", "Could not resolve Year entity.", 0, 0, 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime, executionType, startDate, endDate);
        }

        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        List<ActivityStage> stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(academicYear);

        int processed = 0;
        int eligibleStudentsCount = 0;
        int ineligibleStudentsCount = 0;
        int rewardedStudentsCount = 0;
        int skippedStudentsCount = 0;
        int totalXpAwarded = 0;
        int errors = 0;

        try {
            int perfectWeekReward = settings.getPerfectWeekReward() != null ? settings.getPerfectWeekReward() : 0;

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
                if (!"WEEKLY".equals(rule) && !"BOTH".equals(rule)) {
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
                        long daysWithAttendance = 0;
                        long totalPresent = 0;
                        long totalAbsent = 0;
                        long totalMarked = 0;

                        for (LocalDate workDay : workingDays) {
                            long dPresent = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                    student.getId(), workDay, Attendance.AttendanceStatus.PRESENT);
                            long dAbsent = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                    student.getId(), workDay, Attendance.AttendanceStatus.ABSENT);
                            long dMarked = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(), workDay);

                            if (dMarked > 0) {
                                daysWithAttendance++;
                                totalPresent += dPresent;
                                totalAbsent += dAbsent;
                                totalMarked += dMarked;
                            }
                        }

                        if (totalMarked == 0) {
                            skippedStudentsCount++;
                            continue;
                        }

                        processed++;

                        // Perfect week: marked attendance on EVERY working day, 0 absences, all marked are present
                        boolean isPerfectWeek = (daysWithAttendance == workingDays.size() && totalAbsent == 0 && totalPresent == totalMarked && totalMarked > 0);

                        if (isPerfectWeek) {
                            eligibleStudentsCount++;

                            // Idempotency check against XP transactions for this week
                            String transactionRemark = "Weekly Reward: " + startDate + " to " + endDate;
                            boolean alreadyProcessed = false;
                            List<jjcet.PragatiX.entity.XpTransaction> existingXp = xpTransactionRepository
                                    .findByStudentIdAndActivityId(student.getId(), engineActivity.getId());
                            for (jjcet.PragatiX.entity.XpTransaction xp : existingXp) {
                                if (xp.getActivityName() != null && xp.getActivityName().contains(transactionRemark)) {
                                    alreadyProcessed = true;
                                    break;
                                }
                            }

                            if (!alreadyProcessed) {
                                int awardXp = 0;
                                String ruleApplied = "Perfect Week Reward";

                                if (Boolean.TRUE.equals(engineActivity.getAwardEnabled())) {
                                    awardXp = perfectWeekReward > 0 ? perfectWeekReward
                                            : (engineActivity.getAwardXp() != null ? engineActivity.getAwardXp() : 0);
                                }

                                if (awardXp > 0) {
                                    jjcet.PragatiX.modules.attendance.dto.AttendanceXpExecutionRequest req = new jjcet.PragatiX.modules.attendance.dto.AttendanceXpExecutionRequest();
                                    req.setStudentId(student.getId());
                                    req.setActivityId(engineActivity.getId());
                                    req.setAttendanceRule(ruleApplied);
                                    req.setCalculatedXp(awardXp);
                                    req.setIsPenalty(false);
                                    req.setAttendanceDate(endDate);
                                    req.setWeekStartDate(startDate);
                                    req.setWeekEndDate(endDate);
                                    req.setReason("Attendance Weekly Rule: " + ruleApplied);
                                    req.setRemarks(transactionRemark);

                                    xpEngineService.awardXp(student, engineActivity, null, null, awardXp, transactionRemark, req);

                                    rewardedStudentsCount++;
                                    totalXpAwarded += awardXp;
                                }
                            }
                        } else {
                            ineligibleStudentsCount++;
                        }

                    } catch (Exception e) {
                        log.error("Error processing weekly attendance for student {}: {}", student.getId(), e.getMessage());
                        errors++;
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("SUMMARY");
            log.info("Students Processed : {}", processed);
            log.info("Eligible Students  : {}", eligibleStudentsCount);
            log.info("Ineligible Students: {}", ineligibleStudentsCount);
            log.info("Rewarded Students  : {}", rewardedStudentsCount);
            log.info("Total XP Awarded   : +{} XP", totalXpAwarded);
            log.info("Execution Time     : {} ms", elapsed);
            log.info("WEEKLY ENGINE COMPLETED");

            // Update Execution Record
            execution.setStatus("SUCCESS");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setProcessedCount(processed);
            execution.setPresentCount(eligibleStudentsCount);
            execution.setAbsentCount(ineligibleStudentsCount);
            execution.setPenaltiesApplied(rewardedStudentsCount); // stores award count
            execution.setSkippedCount(skippedStudentsCount);
            execution.setFailureCount(errors);
            execution.setExecutionTimeMs(elapsed);
            executionRepository.save(execution);

            updateEngineStatus(academicYear, "SUCCESS", executionType, "SUCCESS", LocalDateTime.now());

            return buildResult("SUCCESS", "Weekly Attendance Engine completed successfully.",
                    processed, eligibleStudentsCount, ineligibleStudentsCount, rewardedStudentsCount,
                    skippedStudentsCount, errors, elapsed, executionType, startDate, endDate);

        } catch (Exception e) {
            log.error("Weekly Attendance Engine failed: {}", e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            executionRepository.save(execution);

            updateEngineStatus(academicYear, "FAILED", executionType, "FAILED", LocalDateTime.now());

            return buildResult("ERROR", "Engine execution failed: " + e.getMessage(),
                    processed, eligibleStudentsCount, ineligibleStudentsCount, rewardedStudentsCount,
                    skippedStudentsCount, errors + 1, System.currentTimeMillis() - startTime,
                    executionType, startDate, endDate);
        }
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, String runType, String runStatus, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setWeeklyEngineStatus(status);
            if (runType != null) {
                settings.setLastWeeklyRunType(runType);
            }
            if (runStatus != null) {
                settings.setLastWeeklyRunStatus(runStatus);
            }
            if (runTime != null) {
                settings.setLastWeeklyRun(runTime);
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

    private Map<String, Object> buildResult(String status, String message, int total, int eligible, int ineligible,
            int rewarded, int skipped, int errors, long elapsedMs,
            String executionType, LocalDate periodStart, LocalDate periodEnd) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("message", message);
        result.put("processedStudents", total);
        result.put("eligibleStudents", eligible);
        result.put("ineligibleStudents", ineligible);
        result.put("awardsApplied", rewarded);
        result.put("skippedStudents", skipped);
        result.put("failedStudents", errors);
        result.put("executionType", executionType);
        result.put("periodStart", periodStart != null ? periodStart.toString() : null);
        result.put("periodEnd", periodEnd != null ? periodEnd.toString() : null);
        result.put("executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0));
        return result;
    }
}
