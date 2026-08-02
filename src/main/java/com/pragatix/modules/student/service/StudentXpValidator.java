package com.pragatix.modules.student.service;

import com.pragatix.entity.Activity;
import com.pragatix.entity.Student;
import com.pragatix.entity.StudentActivityXp;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentXpValidator {

    private final StudentActivityXpRepository studentActivityXpRepository;
    private final com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver;

    public StudentXpValidator(StudentActivityXpRepository studentActivityXpRepository,
                              com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver) {
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.academicCalendarResolver = academicCalendarResolver;
    }

    public String checkAwardLimit(Student student, Activity activity) {
        String awardFrequency = activity.getAwardFrequency();
        if (awardFrequency == null || awardFrequency.trim().isEmpty()) {
            awardFrequency = "One Time";
        }

        if ("Weekly".equalsIgnoreCase(awardFrequency)) {
            String awardDays = activity.getAwardDays();
            if (awardDays != null && !awardDays.trim().isEmpty()) {
                com.pragatix.enums.AcademicYear academicYear = com.pragatix.enums.AcademicYear.fromStudent(student);
                java.time.DayOfWeek today = academicCalendarResolver.getEffectiveAcademicDay(LocalDate.now(), academicYear);
                if (today == null) {
                    return "Activity cannot be performed. Today is configured as a Holiday.";
                }
                String todayName = today.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
                boolean dayAllowed = java.util.Arrays.stream(awardDays.split(","))
                        .map(String::trim)
                        .anyMatch(d -> d.equalsIgnoreCase(todayName));
                if (!dayAllowed) {
                    String daysFormatted = java.util.Arrays.stream(awardDays.split(","))
                            .map(String::trim).collect(Collectors.joining(", "));
                    return "XP can only be awarded on the configured Award Days: " + daysFormatted + ". Today is "
                            + todayName + ".";
                }
            }
        }

        List<StudentActivityXp> history = studentActivityXpRepository.findByStudentIdAndActivityIdAndStage(
                student.getId(), activity.getId(), student.getStage());

        if ("One Time".equalsIgnoreCase(awardFrequency)) {
            if (!history.isEmpty()) {
                return "Student " + student.getFullName() + " has already been awarded XP for this one-time activity.";
            }
            return null;
        }

        if ("Per Assignment".equalsIgnoreCase(awardFrequency)) {
            return null;
        }

        if ("Manual".equalsIgnoreCase(awardFrequency)) {
            if (!history.isEmpty()) {
                return "Student " + student.getFullName()
                        + " has already been awarded XP for this manual activity. Contact the administrator to reset.";
            }
            return null;
        }

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
            windowStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .atStartOfDay();
            windowLabel = "this week";
        } else if ("Monthly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.withDayOfMonth(1).atStartOfDay();
            windowLabel = "this month";
        } else {
            if (cap == null || cap <= 0) {
                return null;
            } else {
                if (history.size() >= cap) {
                    return "Student " + student.getFullName() + " has reached the maximum cap (" + cap
                            + ") for this activity.";
                }
                return null;
            }
        }

        final LocalDateTime limitStart = windowStart;
        long awardsInWindow = history.stream()
                .filter(h -> !h.getAwardedAt().isBefore(limitStart))
                .count();

        if (awardsInWindow >= cap) {
            return "Student " + student.getFullName() + " has already reached the maximum allowed XP awards ("
                    + cap + ") for " + windowLabel + ".";
        }

        return null;
    }
}
