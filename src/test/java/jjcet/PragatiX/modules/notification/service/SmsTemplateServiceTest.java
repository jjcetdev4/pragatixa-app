package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmsTemplateServiceTest {

    private SmsTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new SmsTemplateService();
    }

    @Test
    void testBuildAbsentStudentMessage_ExactDltFormat() {
        Student student = Student.builder()
                .fullName("Arun Kumar")
                .regNo("920421104001")
                .build();

        LocalDate attendanceDate = LocalDate.of(2026, 8, 21);

        String message = templateService.buildAbsentStudentMessage(student, attendanceDate, 1);

        String expected = "JJECTR -அன்புள்ள பெற்றோரே, தங்கள் பிள்ளை " + student.getFullName() + " இன்று கல்லூரிக்கு வரவில்லை.21/08/2026.Dear Parent, your ward is absent today - JJCET";
        assertEquals(expected, message);
    }

    @Test
    void testBuildAbsentStudentMessage_DatePreservedIndependentOfCurrentDate() {
        Student student = Student.builder()
                .fullName("Priya S")
                .regNo("920421104002")
                .build();

        LocalDate pastDate = LocalDate.of(2026, 8, 21);

        String message = templateService.buildAbsentStudentMessage(student, pastDate);

        String expected = "JJECTR -அன்புள்ள பெற்றோரே, தங்கள் பிள்ளை " + student.getFullName() + " இன்று கல்லூரிக்கு வரவில்லை.21/08/2026.Dear Parent, your ward is absent today - JJCET";
        assertEquals(expected, message);
    }
}
