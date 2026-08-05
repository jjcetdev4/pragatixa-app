package com.pragatix.modules.activity.service;

import com.pragatix.entity.Activity;
import com.pragatix.entity.StudentActivityStreak;
import com.pragatix.entity.Student;
import com.pragatix.repository.StudentActivityStreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.pragatix.modules.activity.repository.ActivityRepository;

@Service
public class ActivityStreakService {

    private final StudentActivityStreakRepository streakRepository;
    private final ActivityRepository activityRepository;

    public ActivityStreakService(StudentActivityStreakRepository streakRepository, ActivityRepository activityRepository) {
        this.streakRepository = streakRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public void incrementStreak(Student student, Activity activity) {
        if (activity == null || !Boolean.TRUE.equals(activity.getStreakEnabled())) {
            return;
        }

        LocalDate today = LocalDate.now();
        Optional<StudentActivityStreak> streakOpt = streakRepository.findByStudentIdAndActivityId(student.getId(), activity.getId());
        StudentActivityStreak streak;

        if (streakOpt.isPresent()) {
            streak = streakOpt.get();
            LocalDate lastDate = streak.getLastCompletedDate();
            
            if (lastDate != null && lastDate.equals(today)) {
                // Already completed today, do nothing to streak count
                return;
            }
            
            // Simplified Consecutive Check based on standard daily progression
            // Ideally, we could parse activity.getResetPeriod() (e.g. "Weekly", "Daily")
            boolean isConsecutive = false;
            
            if (lastDate != null) {
                String resetPeriod = activity.getResetPeriod() != null ? activity.getResetPeriod().trim().toLowerCase() : "daily";
                long daysBetween = ChronoUnit.DAYS.between(lastDate, today);
                
                if (resetPeriod.equals("weekly") || resetPeriod.equals("week")) {
                    isConsecutive = daysBetween <= 7;
                } else if (resetPeriod.equals("monthly") || resetPeriod.equals("month")) {
                    isConsecutive = daysBetween <= 31;
                } else {
                    isConsecutive = daysBetween == 1; // Default to Daily check
                }
            }

            if (isConsecutive) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
            }
        } else {
            streak = new StudentActivityStreak(student, activity);
            streak.setCurrentStreak(1);
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastCompletedDate(today);
        streakRepository.save(streak);
    }
    
    public List<StudentActivityStreak> getStudentActivityStreaks(Long studentId) {
        List<Activity> streakEnabledActivities = activityRepository.findByStreakEnabledTrue();
        List<StudentActivityStreak> existingStreaks = streakRepository.findByStudentId(studentId);

        return streakEnabledActivities.stream().map(activity -> {
            Optional<StudentActivityStreak> existingOpt = existingStreaks.stream()
                    .filter(s -> s.getActivity().getId().equals(activity.getId()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                return existingOpt.get();
            } else {
                StudentActivityStreak dummy = new StudentActivityStreak();
                dummy.setActivity(activity);
                dummy.setCurrentStreak(0);
                dummy.setLongestStreak(0);
                return dummy;
            }
        }).collect(java.util.stream.Collectors.toList());
    }
}
