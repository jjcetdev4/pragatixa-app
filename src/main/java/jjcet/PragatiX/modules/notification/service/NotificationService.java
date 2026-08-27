package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.entity.SmsNotification;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentGuardian;
import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.notification.util.PhoneNumberUtil;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.SmsNotificationRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SmsService smsService;
    private final SmsTemplateService templateService;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SmsNotificationRepository smsNotificationRepository;
    private final SmsProperties smsProperties;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationService(
            SmsService smsService,
            SmsTemplateService templateService,
            StudentRepository studentRepository,
            StudentGuardianRepository studentGuardianRepository,
            SmsNotificationRepository smsNotificationRepository,
            SmsProperties smsProperties) {
        this.smsService = smsService;
        this.templateService = templateService;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.smsNotificationRepository = smsNotificationRepository;
        this.smsProperties = smsProperties;
    }

    /**
     * Sends absentee SMS notification to the student's parent/guardian.
     * Executes asynchronously in an isolated transaction to ensure SMS processing
     * never blocks or rolls back the parent attendance transaction.
     *
     * @param studentId ID of the absent student
     * @param attendanceDate The actual attendance date (used for DLT message and duplicate prevention)
     * @param periodNo The class period number
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAbsenceNotification(Long studentId, LocalDate attendanceDate, Integer periodNo) {
        try {
            if (studentId == null || attendanceDate == null) {
                log.warn("Absence SMS skipped: studentId or attendanceDate is null.");
                return;
            }

            // Duplicate Prevention: Check if absence SMS was already sent for this student and attendance date
            if (smsNotificationRepository.existsByStudentIdAndAttendanceDate(studentId, attendanceDate)) {
                log.info("Absence SMS already sent for student ID {} on date {}. Skipping duplicate.",
                        studentId, attendanceDate.format(DATE_FORMATTER));
                return;
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                log.warn("Absence SMS skipped: Student with ID {} not found.", studentId);
                return;
            }

            // Retrieve parent/guardian (prioritizing primary guardian if multiple exist)
            StudentGuardian guardian = studentGuardianRepository.findFirstByStudentIdOrderByIsPrimaryDesc(studentId)
                    .or(() -> studentGuardianRepository.findByStudentId(studentId))
                    .orElse(null);

            if (guardian == null || guardian.getPhoneNo() == null || guardian.getPhoneNo().trim().isEmpty()) {
                log.warn("Absence SMS skipped: parent mobile unavailable for student {}", student.getRegNo());
                return;
            }

            String rawPhone = guardian.getPhoneNo().trim();
            String normalizedPhone;
            try {
                normalizedPhone = PhoneNumberUtil.normalizeIndianPhoneNumber(rawPhone);
            } catch (Exception e) {
                log.warn("Absence SMS skipped: parent mobile number {} is invalid for student {}: {}",
                        PhoneNumberUtil.maskPhoneNumber(rawPhone), student.getRegNo(), e.getMessage());
                return;
            }

            // Build exact Tamil + English DLT message
            String messageContent = templateService.buildAbsentStudentMessage(student, attendanceDate);

            String activeProvider = (smsProperties != null && smsProperties.getProvider() != null)
                    ? smsProperties.getProvider().name() : "AIRTEL";

            SmsNotification logEntry = new SmsNotification();
            logEntry.setStudentId(student.getId());
            logEntry.setAttendanceDate(attendanceDate);
            logEntry.setPeriodNo(periodNo);
            logEntry.setGuardianPhone(normalizedPhone);
            logEntry.setMessage(messageContent);
            logEntry.setProvider(activeProvider);

            try {
                String messageRequestId = smsService.sendSms(normalizedPhone, messageContent, "ABSENCE");
                logEntry.setStatus("SUCCESS");
                logEntry.setMessageRequestId(messageRequestId);

                log.info("\n================ ABSENT SMS ================\n"
                                + "Student: {}\n"
                                + "Attendance Date: {}\n"
                                + "Parent Mobile: {}\n"
                                + "Provider: {}\n"
                                + "Status: ACCEPTED\n"
                                + "MessageRequestId: {}\n"
                                + "============================================",
                        student.getFullName(),
                        attendanceDate.format(DATE_FORMATTER),
                        PhoneNumberUtil.maskPhoneNumber(normalizedPhone),
                        activeProvider,
                        messageRequestId);

            } catch (Exception e) {
                log.error("Absence SMS failed for student {} (Parent: {}): {}",
                        student.getRegNo(), PhoneNumberUtil.maskPhoneNumber(normalizedPhone), e.getMessage());
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(e.getMessage());
            }

            smsNotificationRepository.save(logEntry);

        } catch (Exception e) {
            log.error("Unexpected error during sendAbsenceNotification for studentId {}", studentId, e);
        }
    }
}
