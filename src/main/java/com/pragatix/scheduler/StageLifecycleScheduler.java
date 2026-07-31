package com.pragatix.scheduler;

import com.pragatix.entity.ActivityStage;
import com.pragatix.enums.StageStatus;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.entity.Notification;
import com.pragatix.entity.Student;
import com.pragatix.repository.NotificationRepository;
import com.pragatix.modules.student.repository.StudentRepository;
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
            StageStatus newStatus;

            if (stage.getStartDateTime() == null && stage.getEndDateTime() == null) {
                newStatus = StageStatus.ACTIVE;
            } else if (stage.getStartDateTime() != null && now.isBefore(stage.getStartDateTime())) {
                newStatus = StageStatus.UPCOMING;
            } else if (stage.getEndDateTime() != null && now.isAfter(stage.getEndDateTime())) {
                newStatus = StageStatus.COMPLETED;
            } else {
                newStatus = StageStatus.ACTIVE;
            }

            if (stage.getStatus() != newStatus) {
                log.debug("Stage '{}' transitioned from {} to {}", stage.getName(), stage.getStatus(), newStatus);
                stage.setStatus(newStatus);
                activityStageRepository.save(stage);
                updated = true;

                if (newStatus == StageStatus.ACTIVE) {
                    notifyStudents("New Stage Started",
                            "Welcome to " + stage.getName() + "! New activities are now available.");
                } else if (newStatus == StageStatus.COMPLETED) {
                    notifyStudents("Stage Locked",
                            "Stage " + stage.getName() + " has ended. Activities are now locked.");
                }
            } else if (newStatus == StageStatus.ACTIVE && stage.getEndDateTime() != null) {
                LocalDateTime tomorrow = now.plusHours(24);
                if (tomorrow.isAfter(stage.getEndDateTime())
                        && tomorrow.minusMinutes(1).isBefore(stage.getEndDateTime())) {
                    notifyStudents("Stage Ending Soon", "Stage " + stage.getName() + " is ending in 24 hours!");
                }
            }
        }

        if (updated) {
            log.debug("Stage lifecycle statuses updated successfully.");
        }
    }

    private void notifyStudents(String title, String message) {
        List<Student> activeStudents = studentRepository.findByActiveTrue();
        List<Notification> notifications = new java.util.ArrayList<>();
        for (Student student : activeStudents) {
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
}
