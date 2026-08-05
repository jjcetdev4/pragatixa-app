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
 * AttendanceWeeklyEngineService - executes the Weekly Attendance Engine for a
 * given Academic Year.
 *
 * Uses EngineClockService to respect Production vs Test Mode.
 * Logs detailed results per student.
 * XP reward transactions will be applied in the next XP Engine step.
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

    @Transactional
    public Map<String, Object> execute(AcademicYear academicYear) {
        long startTime = System.currentTimeMillis();
        LocalDate engineDate = clockService.getEffectiveDate(academicYear);
        boolean testMode = clockService.isTestMode(academicYear);

        AttendanceSettings settings = settingsRepository.findByAcademicYear(academicYear).orElse(null);
        if (settings == null) {
            return buildResult("ERROR", "Settings not found", 0, 0, 0, 0, 0);
        }

        AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(academicYear, engineDate).orElse(null);

        if (activeWeek == null) {
            return buildResult("ERROR", "No active Academic Week configured", 0, 0, 0, 0, 0);
        }

        LocalDate startDate = activeWeek.getStartDate();
        LocalDate endDate = activeWeek.getEndDate();

        if (!engineDate.isEqual(endDate)) {
            return buildResult("SKIPPED", "Engine Date is not End Date", 0, 0, 0, 0, 0);
        }
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");
        String weekStr = startDate.format(formatter) + " - " + endDate.format(formatter);
        
        log.info("========================================");
        log.info("WEEKLY ATTENDANCE ENGINE");
        log.info("Week :");
        log.info(weekStr);
        log.info("========================================");

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

        // Resolve yearId
        byte yearNo = resolveYearNo(academicYear);
        Long yearId = yearRepository.findByYearNo(yearNo).map(y -> y.getId()).orElse(null);
        if (yearId == null) {
            updateEngineStatus(academicYear, "ERROR", null);
            return buildResult("ERROR", "Could not resolve Year entity.", 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime);
        }

        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(s -> s.getYearRef() != null && yearId.equals(s.getYearRef().getId()))
                .collect(Collectors.toList());

        List<ActivityStage> stages = activityStageRepository.findByAcademicYearOrderByDisplayOrderAsc(academicYear);
        
        int errors = 0;
        int eligibleStudentsCount = 0;
        int rewardedStudentsCount = 0;
        int totalXpAwarded = 0;

        try {

            // Get settings for perfect week reward
            int perfectWeekReward = settingsRepository.findByAcademicYear(academicYear)
                    .map(s -> s.getPerfectWeekReward() != null ? s.getPerfectWeekReward() : 0)
                    .orElse(0);

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
                if (!"WEEKLY".equals(rule) && !"BOTH".equals(rule)) {
                    continue;
                }

                List<ActivityAssignment> assignments = activityAssignmentRepository
                        .findByActivityId(engineActivity.getId());
                if (assignments.isEmpty()) {
                    continue;
                }

                int perfectReward = settings.getPerfectWeekReward() != null ? settings.getPerfectWeekReward() : 0;
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
                            totalMarked += attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(),
                                    workDay);
                        }

                        if (totalMarked == 0) {
                            continue;
                        }

                        double attendancePct = totalMarked == 0 ? 0 : (totalPresent * 100.0 / totalMarked);

                        // Root Cause Fix: totalMarked is the number of periods/records across the week,
                        // not days.
                        // We just need to ensure they have some attendance marked, and that all marked
                        // records are PRESENT.
                        // Also, we can optionally ensure they have attendance marked on every working
                        // day.
                        // Let's count how many distinct working days they have attendance for.
                        long daysWithAttendance = 0;
                        long partialDays = 0;
                        long fullAbsentDays = 0;
                        long fullPresentDays = 0;

                        for (LocalDate workDay : workingDays) {
                            long dPresent = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                    student.getId(), workDay, Attendance.AttendanceStatus.PRESENT);
                            long dAbsent = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(
                                    student.getId(), workDay, Attendance.AttendanceStatus.ABSENT);
                            long dMarked = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(),
                                    workDay);

                            if (dMarked > 0) {
                                daysWithAttendance++;
                                if (dAbsent == 0) {
                                    fullPresentDays++;
                                } else if (dPresent > 0) {
                                    partialDays++;
                                } else {
                                    fullAbsentDays++;
                                }
                            }
                        }

                        boolean perfectWeek = false;

                        if (daysWithAttendance < workingDays.size()) {
                            perfectWeek = false;
                        } else if (totalAbsent > 0) {
                            perfectWeek = false;
                        } else if (totalPresent == totalMarked && totalMarked > 0) {
                            perfectWeek = true;
                        } else {
                            perfectWeek = false;
                        }
                        
                        if (perfectWeek) {
                            eligibleStudentsCount++;
                            
                            // Duplicate Protection
                            String transactionRemark = "Weekly Reward: " + startDate + " to " + endDate;
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
                                continue;
                            }

                            int awardXp = 0;
                            String ruleApplied = "";
                            boolean executeXp = false;

                            if (Boolean.TRUE.equals(engineActivity.getAwardEnabled())) {
                                awardXp = perfectReward > 0 ? perfectReward
                                        : (engineActivity.getAwardXp() != null ? engineActivity.getAwardXp() : 0);
                                ruleApplied = "Perfect Week Reward";
                                executeXp = awardXp > 0;
                            } else {
                                ruleApplied = "Perfect Week (Reward Disabled)";
                            }

                            if (executeXp) {
                                try {
                                    com.pragatix.modules.attendance.dto.AttendanceXpExecutionRequest req = new com.pragatix.modules.attendance.dto.AttendanceXpExecutionRequest();
                                    req.setStudentId(student.getId());
                                    req.setActivityId(engineActivity.getId());
                                    req.setAttendanceRule(ruleApplied);
                                    req.setCalculatedXp(awardXp);
                                    req.setIsPenalty(awardXp < 0);
                                    req.setAttendanceDate(endDate);
                                    req.setWeekStartDate(startDate);
                                    req.setWeekEndDate(endDate);
                                    req.setReason("Attendance Weekly Rule: " + ruleApplied);
                                    req.setRemarks(transactionRemark);

                                    Student savedStudent = xpEngineService.awardXp(student, engineActivity, null, null,
                                            awardXp, transactionRemark, req);
                                            
                                    log.info("Student:");
                                    log.info(student.getRegNo());
                                    log.info("");
                                    log.info("Reward:");
                                    log.info("+{} XP", awardXp);
                                    log.info("");
                                    log.info("----------------------------------------");
                                    log.info("");
                                    
                                    rewardedStudentsCount++;
                                    totalXpAwarded += awardXp;
                                    
                                } catch (Exception e) {
                                    log.error("XP Execution Error : {}", e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing student {}: {}", student.getId(), e.getMessage());
                        errors++;
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("SUMMARY");
            log.info("");
            log.info("Students Processed : {}", allStudents.size());
            log.info("");
            log.info("Eligible Students : {}", eligibleStudentsCount);
            log.info("");
            log.info("Rewarded Students : {}", rewardedStudentsCount);
            log.info("");
            log.info("Total XP Awarded : +{}", totalXpAwarded);
            log.info("");
            log.info("Execution Time : {} seconds", String.format("%.1f", elapsed / 1000.0));
            log.info("");
            log.info("WEEKLY ENGINE COMPLETED");

            if (rewardedStudentsCount == 0) {
                updateEngineStatus(academicYear, "NO DATA PROCESSED", LocalDateTime.now());
                return buildResult("SUCCESS", "Weekly engine completed but no students were processed.",
                        allStudents.size(), 0, allStudents.size() - rewardedStudentsCount, errors, elapsed);
            } else {
                updateEngineStatus(academicYear, "DONE", LocalDateTime.now());
                return buildResult("SUCCESS", "Weekly engine completed successfully.",
                        allStudents.size(), rewardedStudentsCount, allStudents.size() - rewardedStudentsCount, errors, elapsed);
            }

        } catch (Exception e) {
            log.error("AttendanceService failed");
            log.error("Exception: {}", e.getMessage(), e);
            updateEngineStatus(academicYear, "FAILED", null);
            return buildResult("ERROR", "Engine execution failed: " + e.getMessage(), 0, 0, 0, 1,
                    System.currentTimeMillis() - startTime);
        }
    }

    private void updateEngineStatus(AcademicYear academicYear, String status, LocalDateTime runTime) {
        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setWeeklyEngineStatus(status);
            if (runTime != null)
                settings.setLastWeeklyRun(runTime);
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

    private Map<String, Object> buildResult(String status, String message, int total, int rewarded, int notEligible,
            int errors, long elapsedMs) {
        return Map.of(
                "status", status,
                "message", message,
                "totalStudents", total,
                "rewarded", rewarded,
                "notEligible", notEligible,
                "errors", errors,
                "executionTimeSeconds", String.format("%.1f", elapsedMs / 1000.0));
    }
}
