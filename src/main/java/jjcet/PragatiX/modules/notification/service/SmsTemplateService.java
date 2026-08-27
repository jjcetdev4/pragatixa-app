package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.entity.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SmsTemplateService {

    /**
     * Exact Airtel DLT registered bilingual absentee template.
     * Contains both Tamil and English messages with only {STUDENT_NAME} and {DATE} placeholders.
     */
    private static final String DLT_ABSENTEE_TEMPLATE =
            "JJECTR -அன்புள்ள பெற்றோரே, தங்கள் பிள்ளை {STUDENT_NAME} இன்று கல்லூரிக்கு வரவில்லை.{DATE}.Dear Parent, your ward is absent today - JJCET";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Builds the exact DLT absentee message replacing {STUDENT_NAME} and {DATE}.
     *
     * @param student The absent Student record
     * @param attendanceDate The actual attendance date (formatted as dd/MM/yyyy)
     * @return Fully formatted DLT SMS message text
     */
    public String buildAbsentStudentMessage(Student student, LocalDate attendanceDate) {
        String formattedDate = (attendanceDate != null) ? attendanceDate.format(DATE_FORMATTER) : "";
        String studentName = (student != null && student.getFullName() != null && !student.getFullName().trim().isEmpty())
                ? student.getFullName().trim()
                : (student != null && student.getRegNo() != null ? student.getRegNo() : "");

        return DLT_ABSENTEE_TEMPLATE
                .replace("{STUDENT_NAME}", studentName)
                .replace("{DATE}", formattedDate);
    }

    /**
     * Overload for backward compatibility accepting periodNo (which is not in the DLT template).
     */
    public String buildAbsentStudentMessage(Student student, LocalDate attendanceDate, Integer periodNo) {
        return buildAbsentStudentMessage(student, attendanceDate);
    }
}
