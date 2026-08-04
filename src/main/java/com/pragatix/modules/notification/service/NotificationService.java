package com.pragatix.modules.notification.service;

import com.pragatix.entity.SmsNotification;
import com.pragatix.entity.Student;
import com.pragatix.entity.StudentGuardian;
import com.pragatix.repository.SmsNotificationRepository;
import com.pragatix.repository.StudentGuardianRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.LocalDate;

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
    public void sendAbsenceNotification(Long studentId, LocalDate date) {
        try {
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
            String messageContent = templateService.buildAbsentStudentMessage(student, date);

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
