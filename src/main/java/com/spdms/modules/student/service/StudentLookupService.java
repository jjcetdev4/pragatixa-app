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

@Service
public class StudentLookupService {
    private static final Logger log = LoggerFactory.getLogger(StudentLookupService.class);

    private final AcademicYearRepository academicYearRepository;
    private final DepartmentRepository departmentRepository;
    private final GenderRepository genderRepository;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final YearRepository yearRepository;

    public StudentLookupService(AcademicYearRepository academicYearRepository,
            DepartmentRepository departmentRepository, GenderRepository genderRepository,
            SectionRepository sectionRepository, SemesterRepository semesterRepository, YearRepository yearRepository) {
        this.academicYearRepository = academicYearRepository;
        this.departmentRepository = departmentRepository;
        this.genderRepository = genderRepository;
        this.sectionRepository = sectionRepository;
        this.semesterRepository = semesterRepository;
        this.yearRepository = yearRepository;
    }

    public Department resolveDepartment(Long id, String name) {
        if (id != null) {
            return departmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        } else if (name != null && !name.trim().isEmpty()) {
            String trimmedName = name.trim();
            return departmentRepository.findByName(trimmedName)
                    .or(() -> departmentRepository.findByDeptCode(trimmedName))
                    .or(() -> departmentRepository.findByCode(trimmedName))
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        } else {
            throw new IllegalArgumentException("Department is required");
        }
    }

    public Gender resolveGender(Long id, String name) {
        if (id != null) {
            return genderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Gender not found"));
        } else if (name != null && !name.trim().isEmpty()) {
            return genderRepository.findByGenderName(name.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Gender not found"));
        } else {
            throw new IllegalArgumentException("Gender is required");
        }
    }

    public Year resolveYear(Long id, String name) {
        if (id != null) {
            return yearRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Year not found"));
        } else if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            Byte yearNo = null;
            try {
                yearNo = Byte.parseByte(trimmed);
            } catch (NumberFormatException e) {
                // Ignore
            }
            if (yearNo != null) {
                return yearRepository.findByYearNo(yearNo)
                        .orElseThrow(() -> new IllegalArgumentException("Year not found"));
            }
            String lower = trimmed.toLowerCase();
            byte matchNo = 0;
            if (lower.contains("first") || lower.contains("1"))
                matchNo = 1;
            else if (lower.contains("second") || lower.contains("2"))
                matchNo = 2;
            else if (lower.contains("third") || lower.contains("3"))
                matchNo = 3;
            else if (lower.contains("fourth") || lower.contains("4"))
                matchNo = 4;

            if (matchNo > 0) {
                return yearRepository.findByYearNo(matchNo)
                        .orElseThrow(() -> new IllegalArgumentException("Year not found"));
            }

            return yearRepository.findByYearName(trimmed)
                    .orElseThrow(() -> new IllegalArgumentException("Year not found"));
        } else {
            throw new IllegalArgumentException("Year is required");
        }
    }

    public Semester resolveSemester(Long id, String name) {
        if (id != null) {
            return semesterRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        } else if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            Byte semesterNo = null;
            try {
                semesterNo = Byte.parseByte(trimmed);
            } catch (NumberFormatException e) {
                // Ignore
            }
            if (semesterNo != null) {
                return semesterRepository.findBySemesterNo(semesterNo)
                        .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
            }
            for (byte i = 1; i <= 8; i++) {
                if (trimmed.contains(String.valueOf(i))) {
                    return semesterRepository.findBySemesterNo(i)
                            .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
                }
            }
            return semesterRepository.findBySemesterName(trimmed)
                    .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        } else {
            throw new IllegalArgumentException("Semester is required");
        }
    }

    public AcademicYear resolveAcademicYear(Long id, String name) {
        if (id != null) {
            return academicYearRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Academic Year not found"));
        } else if (name != null && !name.trim().isEmpty()) {
            return academicYearRepository.findByAcademicYear(name.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Academic Year not found"));
        } else {
            throw new IllegalArgumentException("Academic Year is required");
        }
    }

    public Section resolveSection(Long id, String name, Department department) {
        if (id != null) {
            return sectionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        } else if (name != null && !name.trim().isEmpty() && department != null) {
            return sectionRepository.findByDepartmentAndSectionName(department, name.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        }
        return null;
    }

    public String normalizeAcademicYear(String input) {
        if (input == null)
            return "";
        String cleaned = input.replaceAll("\\s+", ""); // Remove all spaces
        if (cleaned.matches("\\d{4}-\\d{2}")) { // e.g. "2024-25"
            String start = cleaned.substring(0, 4);
            String endPrefix = cleaned.substring(0, 2);
            String endSuffix = cleaned.substring(5, 7);
            return start + "-" + endPrefix + endSuffix; // becomes "2024-2025"
        }
        return cleaned;
    }

}
