package com.pragatix.modules.notification.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SmsTemplateService {

    private static final String BILINGUAL_ABSENT_TEMPLATE = " வருகைப் பதிவு\n\n" +
            "{STUDENT_NAME} இன்று {DATE} வகுப்பில் இல்லை.\n\n" +
            "CC-ஐ தொடர்பு கொள்ளவும்.\n\n" +
            "JJ College\n\n";

    public String buildAbsentStudentMessage(com.pragatix.entity.Student student, LocalDate attendanceDate) {
        String formattedDate = attendanceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String studentName = student != null && student.getFullName() != null ? student.getFullName() : "";

        return BILINGUAL_ABSENT_TEMPLATE
                .replace("{STUDENT_NAME}", studentName)
                .replace("{DATE}", formattedDate);
    }
}
