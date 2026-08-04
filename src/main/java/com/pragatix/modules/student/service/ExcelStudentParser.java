package com.pragatix.modules.student.service;

import com.pragatix.dto.*;
import com.pragatix.modules.activity.dto.request.*;
import com.pragatix.modules.activity.dto.response.*;
import com.pragatix.modules.student.dto.request.*;
import com.pragatix.modules.student.dto.response.*;
import com.pragatix.entity.*;
import com.pragatix.repository.*;
import com.pragatix.modules.activity.repository.*;
import com.pragatix.modules.faculty.repository.*;
import com.pragatix.modules.student.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ExcelStudentParser {
    private static final Logger log = LoggerFactory.getLogger(ExcelStudentParser.class);

    public LocalDate parseLocalDate(Cell cell) {
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty())
                return null;
            try {
                return LocalDate.parse(val); // Try YYYY-MM-DD
            } catch (Exception e) {
                try {
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                } catch (Exception ex) {
                    try {
                        return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (Exception exc) {
                        try {
                            return LocalDate.parse(val, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        } catch (Exception exc2) {
                            log.warn("Unable to parse DOB string: {}", val);
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.format("%.0f", cell.getNumericCellValue()).trim();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue()).trim();
        }
        return "";
    }

    public String generateShortCode(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "DEPT";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        String[] words = cleaned.split("\\s+");
        if (words.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty() && !w.equalsIgnoreCase("and") && !w.equalsIgnoreCase("of")
                        && !w.equalsIgnoreCase("the")) {
                    sb.append(w.charAt(0));
                }
            }
            String code = sb.toString().toUpperCase();
            return code.length() > 10 ? code.substring(0, 10) : (code.isEmpty() ? "DEPT" : code);
        } else {
            String code = cleaned.toUpperCase();
            return code.length() > 10 ? code.substring(0, 10) : (code.isEmpty() ? "DEPT" : code);
        }
    }

}
