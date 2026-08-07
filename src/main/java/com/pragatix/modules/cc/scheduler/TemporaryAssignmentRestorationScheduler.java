package com.pragatix.modules.cc.scheduler;

import com.pragatix.entity.ActivityTemporaryAssignment;
import com.pragatix.repository.ActivityTemporaryAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TemporaryAssignmentRestorationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TemporaryAssignmentRestorationScheduler.class);

    private final ActivityTemporaryAssignmentRepository temporaryAssignmentRepository;

    public TemporaryAssignmentRestorationScheduler(ActivityTemporaryAssignmentRepository temporaryAssignmentRepository) {
        this.temporaryAssignmentRepository = temporaryAssignmentRepository;
    }

    /**
     * Runs daily at midnight and every 15 minutes to auto-restore permanent teachers
     * by expiring temporary assignments whose expiry date has passed.
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    @Transactional
    public void autoRestoreExpiredTemporaryAssignments() {
        LocalDate today = LocalDate.now();
        List<ActivityTemporaryAssignment> expiredList = temporaryAssignmentRepository.findByStatusAndExpiryDateLessThan("ACTIVE", today);

        if (expiredList.isEmpty()) {
            return;
        }

        log.info("========================================");
        log.info("AUTOMATED TEMPORARY ASSIGNMENT RESTORATION");
        log.info("Found {} temporary assignments to expire/restore", expiredList.size());

        for (ActivityTemporaryAssignment temp : expiredList) {
            temp.setStatus("EXPIRED");
            temp.setUpdatedAt(LocalDateTime.now());
            temporaryAssignmentRepository.save(temp);

            String origTeacherName = temp.getOriginalTeacher() != null ? temp.getOriginalTeacher().getFullName() : "Default / Unassigned";
            String tempTeacherName = temp.getTemporaryTeacher() != null ? temp.getTemporaryTeacher().getFullName() : "Unknown";
            String actName = temp.getActivity() != null ? temp.getActivity().getName() : ("ID: " + temp.getActivity().getId());

            log.info("Restored Permanent Assignment -> Activity: [{}], Temp Teacher: [{}] expired, Restored to: [{}]",
                    actName, tempTeacherName, origTeacherName);
        }

        log.info("Temporary assignment restoration complete.");
        log.info("========================================");
    }
}
