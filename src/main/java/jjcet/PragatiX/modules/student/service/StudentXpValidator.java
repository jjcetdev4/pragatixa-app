package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentActivityXp;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.repository.TeamRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentXpValidator {

    private final StudentActivityXpRepository studentActivityXpRepository;
    private final jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver;
    private final TeamRepository teamRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;

    public StudentXpValidator(StudentActivityXpRepository studentActivityXpRepository,
            jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver,
            TeamRepository teamRepository,
            ActivityAssignmentRepository activityAssignmentRepository) {
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.academicCalendarResolver = academicCalendarResolver;
        this.teamRepository = teamRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
    }

    public String checkAwardLimit(Student student, Activity activity) {
        String awardFrequency = activity.getAwardFrequency();
        if (awardFrequency == null || awardFrequency.trim().isEmpty()) {
            awardFrequency = "One Time";
        }

        // Resolve Assignment ID
        Long assignmentId = null;
        if (activityAssignmentRepository != null) {
            java.util.List<jjcet.PragatiX.entity.ActivityAssignment> assignments = activityAssignmentRepository
                    .findByActivityId(activity.getId());
            jjcet.PragatiX.entity.ActivityAssignment assignment = assignments.stream()
                    .filter(a -> {
                        if (a.getAssignmentScope() == jjcet.PragatiX.entity.AssignmentScope.GLOBAL)
                            return true;
                        if (a.getAssignmentScope() == jjcet.PragatiX.entity.AssignmentScope.DEPARTMENT
                                && student.getDepartment() != null && a.getDepartment() != null
                                && student.getDepartment().getId().equals(a.getDepartment().getId()))
                            return true;
                        if (a.getAssignmentScope() == jjcet.PragatiX.entity.AssignmentScope.SECTION
                                && student.getSection() != null && a.getSection() != null
                                && student.getSection().getId().equals(a.getSection().getId()))
                            return true;
                        return false;
                    })
                    .findFirst().orElse(null);
            if (assignment != null) {
                assignmentId = assignment.getId();
            }
        }

        // Resolve Team ID
        Long teamId = null;
        if (teamRepository != null) {
            jjcet.PragatiX.entity.Team team = teamRepository.findTeamByStudentId(student.getId()).orElse(null);
            if (team != null) {
                teamId = team.getId();
            }
        }

        List<StudentActivityXp> history = studentActivityXpRepository.findByStudentIdAndActivityIdAndStage(
                student.getId(), activity.getId(), student.getStage());

        boolean completionExists = !history.isEmpty();
        boolean transactionExists = !history.isEmpty();

        // Determine if this is a group activity
        boolean isGroupActivity = activity.isGroupXpEligible()
                || (activity.getSubgroup() != null && "group".equalsIgnoreCase(activity.getSubgroup().getCategory()))
                || "GROUP".equalsIgnoreCase(activity.getModeType());

        // Repeatable logic:
        // 1. If activity.isRepeatAllowed() is true -> repeatable.
        // 2. If it is a group activity, and not explicitly marked as One Time or Manual
        // -> default behavior is repeatable.
        // 3. Otherwise -> not repeatable.
        boolean repeatableFlag = activity.isRepeatAllowed();
        boolean isRepeatable = repeatableFlag
                || (isGroupActivity && !"One Time".equalsIgnoreCase(activity.getAwardFrequency())
                        && !"Manual".equalsIgnoreCase(activity.getAwardFrequency()));

        String validationDecision = "ALLOW";
        String validationError = null;

        if (isRepeatable) {
            validationDecision = "ALLOW";
            validationError = null;
        } else {
            if ("Weekly".equalsIgnoreCase(awardFrequency)) {
                String awardDays = activity.getAwardDays();
                if (awardDays != null && !awardDays.trim().isEmpty()) {
                    jjcet.PragatiX.enums.AcademicYear academicYear = jjcet.PragatiX.enums.AcademicYear
                            .fromStudent(student);
                    java.time.DayOfWeek today = academicCalendarResolver.getEffectiveAcademicDay(LocalDate.now(),
                            academicYear);
                    if (today == null) {
                        validationDecision = "BLOCK (Holiday)";
                        validationError = "Activity cannot be performed. Today is configured as a Holiday.";
                    } else {
                        String todayName = today.getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale.ENGLISH);
                        boolean dayAllowed = java.util.Arrays.stream(awardDays.split(","))
                                .map(String::trim)
                                .anyMatch(d -> d.equalsIgnoreCase(todayName));
                        if (!dayAllowed) {
                            String daysFormatted = java.util.Arrays.stream(awardDays.split(","))
                                    .map(String::trim).collect(Collectors.joining(", "));
                            validationDecision = "BLOCK (Day not allowed)";
                            validationError = "XP can only be awarded on the configured Award Days: " + daysFormatted
                                    + ". Today is "
                                    + todayName + ".";
                        }
                    }
                }
            }

            if (validationError == null) {
                if ("One Time".equalsIgnoreCase(awardFrequency)) {
                    if (!history.isEmpty()) {
                        validationDecision = "BLOCK (One-time limit reached)";
                        validationError = "Student " + student.getFullName()
                                + " has already been awarded XP for this one-time activity.";
                    }
                } else if ("Per Assignment".equalsIgnoreCase(awardFrequency)) {
                    validationDecision = "ALLOW";
                    validationError = null;
                } else if ("Manual".equalsIgnoreCase(awardFrequency)) {
                    if (!history.isEmpty()) {
                        validationDecision = "BLOCK (Manual limit reached)";
                        validationError = "Student " + student.getFullName()
                                + " has already been awarded XP for this manual activity. Contact the administrator to reset.";
                    }
                } else {
                    Integer cap = activity.getMaximumAwards();
                    if (cap == null || cap <= 0)
                        cap = 1;

                    LocalDate now = LocalDate.now();
                    LocalDateTime windowStart;
                    String windowLabel;

                    if ("Daily".equalsIgnoreCase(awardFrequency)) {
                        windowStart = now.atStartOfDay();
                        windowLabel = "today";
                    } else if ("Every Period".equalsIgnoreCase(awardFrequency)) {
                        windowStart = now.atStartOfDay();
                        windowLabel = "today";
                        cap = 8;
                    } else if ("Weekly".equalsIgnoreCase(awardFrequency)) {
                        windowStart = now
                                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                .atStartOfDay();
                        windowLabel = "this week";
                    } else if ("Monthly".equalsIgnoreCase(awardFrequency)) {
                        windowStart = now.withDayOfMonth(1).atStartOfDay();
                        windowLabel = "this month";
                    } else {
                        if (cap == null || cap <= 0) {
                            validationDecision = "ALLOW";
                            validationError = null;
                        } else {
                            if (history.size() >= cap) {
                                validationDecision = "BLOCK (Cap reached)";
                                validationError = "Student " + student.getFullName() + " has reached the maximum cap ("
                                        + cap
                                        + ") for this activity.";
                            }
                        }
                        windowStart = null;
                        windowLabel = null;
                    }

                    if (validationError == null && windowStart != null) {
                        final LocalDateTime limitStart = windowStart;
                        long awardsInWindow = history.stream()
                                .filter(h -> !h.getAwardedAt().isBefore(limitStart))
                                .count();

                        if (awardsInWindow >= cap) {
                            validationDecision = "BLOCK (Window cap reached)";
                            validationError = "Student " + student.getFullName()
                                    + " has already reached the maximum allowed XP awards ("
                                    + cap + ") for " + windowLabel + ".";
                        }
                    }
                }
            }
        }

        // Print Debug Logging exactly as requested
        System.out.println("Activity ID : " + activity.getId());
        System.out.println("Assignment ID : " + (assignmentId != null ? assignmentId : "null"));
        System.out.println("Team ID : " + (teamId != null ? teamId : "null"));
        System.out.println("Completion Exists : " + completionExists);
        System.out.println("Transaction Exists : " + transactionExists);
        System.out.println("Repeatable : " + isRepeatable);
        System.out.println("Decision : " + validationDecision);

        return validationError;
    }
}
