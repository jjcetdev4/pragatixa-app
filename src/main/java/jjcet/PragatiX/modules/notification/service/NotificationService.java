package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.entity.SmsNotification;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentGuardian;
import jjcet.PragatiX.repository.SmsNotificationRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SmsService smsService;
    private final SmsTemplateService templateService;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SmsNotificationRepository smsNotificationRepository;

    public NotificationService(
            SmsService smsService,
            SmsTemplateService templateService,
            StudentRepository studentRepository,
            StudentGuardianRepository studentGuardianRepository,
            SmsNotificationRepository smsNotificationRepository) {
        this.smsService = smsService;
        this.templateService = templateService;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.smsNotificationRepository = smsNotificationRepository;
    }

    /**
     * Executes asynchronously and in a completely new transaction to ensure
     * it does not interfere with the calling Attendance transaction.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAbsenceNotification(Long studentId, LocalDate date, Integer periodNo) {
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59, 999999999);

            if (smsNotificationRepository.existsByStudentIdAndCreatedAtBetween(studentId, startOfDay, endOfDay)) {
                log.info("SMS already sent today to student {}. Skipping.", studentId);
                return;
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                log.warn("Student {} not found for SMS notification", studentId);
                return;
            }

            StudentGuardian guardian = studentGuardianRepository.findByStudentId(studentId).orElse(null);
            if (guardian == null || guardian.getPhoneNo() == null || guardian.getPhoneNo().trim().isEmpty()) {
                log.warn("No valid guardian phone found for student {}", student.getRegNo());
                return;
            }

            String phone = guardian.getPhoneNo().trim();
            String messageContent = templateService.buildAbsentStudentMessage(student, date, periodNo);

            SmsNotification logEntry = new SmsNotification();
            logEntry.setStudentId(student.getId());
            logEntry.setGuardianPhone(phone);
            logEntry.setMessage(messageContent);
            logEntry.setProvider("TWILIO");

            try {
                String sid = smsService.sendSms(phone, messageContent);
                logEntry.setStatus("SUCCESS");
                logEntry.setTwilioSid(sid);
            } catch (Exception e) {
                log.error("SMS sending failed for student {}", student.getRegNo(), e);
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(e.getMessage());
            }

            smsNotificationRepository.save(logEntry);

        } catch (Exception e) {
            log.error("Fatal error during sendAbsenceNotification for student {}", studentId, e);
        }
    }
}
