package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.entity.SmsNotification;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentGuardian;
import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.SmsNotificationRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private SmsService smsService;
    private SmsTemplateService templateService;
    private StudentRepository studentRepository;
    private StudentGuardianRepository studentGuardianRepository;
    private SmsNotificationRepository smsNotificationRepository;
    private SmsProperties smsProperties;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        smsService = mock(SmsService.class);
        templateService = new SmsTemplateService();
        studentRepository = mock(StudentRepository.class);
        studentGuardianRepository = mock(StudentGuardianRepository.class);
        smsNotificationRepository = mock(SmsNotificationRepository.class);
        smsProperties = new SmsProperties();
        smsProperties.setProvider(SmsProperties.SmsProvider.AIRTEL);

        notificationService = new NotificationService(
                smsService,
                templateService,
                studentRepository,
                studentGuardianRepository,
                smsNotificationRepository,
                smsProperties
        );
    }

    @Test
    void testSendAbsenceNotification_SuccessWithExactDltMessage() {
        Student student = Student.builder()
                .fullName("Arun Kumar")
                .regNo("920421104001")
                .build();
        student.setId(101L);

        StudentGuardian guardian = StudentGuardian.builder()
                .student(student)
                .guardianName("Kumar (Father)")
                .phoneNo("9591234567")
                .isPrimary(true)
                .build();

        LocalDate attendanceDate = LocalDate.of(2026, 8, 21);

        when(smsNotificationRepository.existsByStudentIdAndAttendanceDate(101L, attendanceDate)).thenReturn(false);
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(studentGuardianRepository.findFirstByStudentIdOrderByIsPrimaryDesc(101L)).thenReturn(Optional.of(guardian));
        when(smsService.sendSms(eq("919591234567"), anyString(), eq("ABSENCE"))).thenReturn("REQ_AIRTEL_999");

        notificationService.sendAbsenceNotification(101L, attendanceDate, 1);

        String expectedMessage = "JJECTR -அன்புள்ள பெற்றோரே, தங்கள் பிள்ளை " + student.getFullName() + " இன்று கல்லூரிக்கு வரவில்லை.21/08/2026.Dear Parent, your ward is absent today - JJCET";
        verify(smsService, times(1)).sendSms("919591234567", expectedMessage, "ABSENCE");

        ArgumentCaptor<SmsNotification> captor = ArgumentCaptor.forClass(SmsNotification.class);
        verify(smsNotificationRepository, times(1)).save(captor.capture());

        SmsNotification saved = captor.getValue();
        assertEquals(101L, saved.getStudentId());
        assertEquals("919591234567", saved.getGuardianPhone());
        assertEquals(expectedMessage, saved.getMessage());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals("REQ_AIRTEL_999", saved.getMessageRequestId());
        assertEquals(attendanceDate, saved.getAttendanceDate());
    }

    @Test
    void testSendAbsenceNotification_DuplicatePrevented() {
        LocalDate attendanceDate = LocalDate.of(2026, 8, 21);
        when(smsNotificationRepository.existsByStudentIdAndAttendanceDate(101L, attendanceDate)).thenReturn(true);

        notificationService.sendAbsenceNotification(101L, attendanceDate, 1);

        verify(smsService, never()).sendSms(anyString(), anyString(), anyString());
        verify(smsNotificationRepository, never()).save(any(SmsNotification.class));
    }

    @Test
    void testSendAbsenceNotification_MissingGuardianPhone_SkippedSafely() {
        Student student = Student.builder()
                .fullName("Arun Kumar")
                .regNo("920421104001")
                .build();
        student.setId(101L);

        LocalDate attendanceDate = LocalDate.of(2026, 8, 21);
        when(smsNotificationRepository.existsByStudentIdAndAttendanceDate(101L, attendanceDate)).thenReturn(false);
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(studentGuardianRepository.findFirstByStudentIdOrderByIsPrimaryDesc(101L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.sendAbsenceNotification(101L, attendanceDate, 1));
        verify(smsService, never()).sendSms(anyString(), anyString(), anyString());
    }

    @Test
    void testSendAbsenceNotification_ProviderFailureHandledGracefully() {
        Student student = Student.builder()
                .fullName("Arun Kumar")
                .regNo("920421104001")
                .build();
        student.setId(101L);

        StudentGuardian guardian = StudentGuardian.builder()
                .student(student)
                .guardianName("Kumar (Father)")
                .phoneNo("9591234567")
                .isPrimary(true)
                .build();

        LocalDate attendanceDate = LocalDate.of(2026, 8, 21);

        when(smsNotificationRepository.existsByStudentIdAndAttendanceDate(101L, attendanceDate)).thenReturn(false);
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(studentGuardianRepository.findFirstByStudentIdOrderByIsPrimaryDesc(101L)).thenReturn(Optional.of(guardian));
        when(smsService.sendSms(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Airtel connection timeout"));

        assertDoesNotThrow(() -> notificationService.sendAbsenceNotification(101L, attendanceDate, 1));

        ArgumentCaptor<SmsNotification> captor = ArgumentCaptor.forClass(SmsNotification.class);
        verify(smsNotificationRepository, times(1)).save(captor.capture());

        SmsNotification saved = captor.getValue();
        assertEquals("FAILED", saved.getStatus());
        assertTrue(saved.getErrorMessage().contains("Airtel connection timeout"));
    }
}
