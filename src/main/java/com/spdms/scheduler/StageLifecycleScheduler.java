package com.spdms.scheduler;

import com.spdms.entity.ActivityStage;
import com.spdms.enums.StageStatus;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.entity.Notification;
import com.spdms.entity.Student;
import com.spdms.repository.NotificationRepository;
import com.spdms.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StageLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(StageLifecycleScheduler.class);
    private final ActivityStageRepository activityStageRepository;
    private final StudentRepository studentRepository;
    private final NotificationRepository notificationRepository;

    public StageLifecycleScheduler(ActivityStageRepository activityStageRepository,
                                   StudentRepository studentRepository,
                                   NotificationRepository notificationRepository) {
        this.activityStageRepository = activityStageRepository;
        this.studentRepository = studentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateStageStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<ActivityStage> allStages = activityStageRepository.findAll();
        boolean updated = false;

        for (ActivityStage stage : allStages) {
            if (stage.getStartDateTime() == null || stage.getEndDateTime() == null) {
                continue;
            }

            StageStatus newStatus = StageStatus.ACTIVE;

            if (stage.getStatus() != newStatus) {
                log.debug("Stage '{}' transitioned from {} to {}", stage.getName(), stage.getStatus(), newStatus);
                stage.setStatus(newStatus);
                activityStageRepository.save(stage);
                updated = true;

                if (newStatus == StageStatus.ACTIVE) {
                    notifyStudents("New Stage Started", "Welcome to " + stage.getName() + "! New activities are now available.", stage.getAssignedAcademicYear());
                } else if (newStatus == StageStatus.COMPLETED) {
                    notifyStudents("Stage Locked", "Stage " + stage.getName() + " has ended. Activities are now locked.", stage.getAssignedAcademicYear());
                }
            } else if (newStatus == StageStatus.ACTIVE && stage.getEndDateTime() != null) {
                LocalDateTime tomorrow = now.plusHours(24);
                if (tomorrow.isAfter(stage.getEndDateTime()) && tomorrow.minusMinutes(1).isBefore(stage.getEndDateTime())) {
                    notifyStudents("Stage Ending Soon", "Stage " + stage.getName() + " is ending in 24 hours!", stage.getAssignedAcademicYear());
                }
            }
        }
        
        if (updated) {
            log.debug("Stage lifecycle statuses updated successfully.");
        }
    }

    private void notifyStudents(String title, String message, com.spdms.entity.AssignedAcademicYear assignedAcademicYear) {
        List<Student> activeStudents = studentRepository.findByActiveTrue();
        List<Notification> notifications = new java.util.ArrayList<>();
        for (Student student : activeStudents) {
            // Check if student belongs to the stage's year
            if (getStudentAssignedYear(student) != assignedAcademicYear) {
                continue;
            }
            Notification notification = Notification.builder()
                    .title(title)
                    .message(message)
                    .student(student)
                    .referenceType("SYSTEM")
                    .incidentDate(LocalDateTime.now())
                    .isRead(false)
                    .build();
            notifications.add(notification);
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private com.spdms.entity.AssignedAcademicYear getStudentAssignedYear(Student student) {
        if (student.getYearRef() != null) {
            Byte yearNo = student.getYearRef().getYearNo();
            if (yearNo != null) {
                switch (yearNo) {
                    case 1: return com.spdms.entity.AssignedAcademicYear.FIRST_YEAR;
                    case 2: return com.spdms.entity.AssignedAcademicYear.SECOND_YEAR;
                    case 3: return com.spdms.entity.AssignedAcademicYear.THIRD_YEAR;
                    case 4: return com.spdms.entity.AssignedAcademicYear.FOURTH_YEAR;
                }
            }
        }
        return com.spdms.entity.AssignedAcademicYear.FIRST_YEAR;
    }
}
