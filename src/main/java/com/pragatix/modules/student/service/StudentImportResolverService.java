package com.pragatix.modules.student.service;

import com.pragatix.entity.*;
import com.pragatix.repository.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentImportResolverService {

    private static final Logger log = LoggerFactory.getLogger(StudentImportResolverService.class);

    private final AcademicYearRepository academicYearRepository;
    private final DepartmentRepository departmentRepository;
    private final GenderRepository genderRepository;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final YearRepository yearRepository;
    private final StudentLookupService studentLookupService;

    public StudentImportResolverService(AcademicYearRepository academicYearRepository,
            DepartmentRepository departmentRepository,
            GenderRepository genderRepository,
            SectionRepository sectionRepository,
            SemesterRepository semesterRepository,
            YearRepository yearRepository,
            StudentLookupService studentLookupService) {
        this.academicYearRepository = academicYearRepository;
        this.departmentRepository = departmentRepository;
        this.genderRepository = genderRepository;
        this.sectionRepository = sectionRepository;
        this.semesterRepository = semesterRepository;
        this.yearRepository = yearRepository;
        this.studentLookupService = studentLookupService;
    }

    private String normalizeBasic(String input) {
        if (input == null)
            return "";
        return input.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String extractDigits(String input) {
        if (input == null)
            return "";
        return input.replaceAll("[^0-9]", "");
    }

    public Long resolveDepartment(String deptName) {
        if (deptName == null || deptName.trim().isEmpty())
            return null;
        String normInput = normalizeBasic(deptName);
        List<Department> allDepts = departmentRepository.findAll();

        for (Department d : allDepts) {
            String dbName = normalizeBasic(d.getName());
            String dbCode = normalizeBasic(d.getCode());
            String dbDeptCode = normalizeBasic(d.getDeptCode());

            if (dbName.equals(normInput) || dbCode.equals(normInput) || dbDeptCode.equals(normInput)) {
                log.info("Department Input: {} | Database Match: {} | Resolved ID: {}", deptName, d.getName(),
                        d.getId());
                return d.getId();
            }
        }

        List<String> avail = allDepts.stream().map(Department::getName).collect(Collectors.toList());
        log.warn("Cannot resolve Department\nInput: {}\nAvailable database values: {}\nResolved ID: NULL", deptName,
                avail);
        return null;
    }

    public Long resolveGender(String genderName) {
        if (genderName == null || genderName.trim().isEmpty())
            return getDefaultGender();
        String normInput = normalizeBasic(genderName);
        List<Gender> allGenders = genderRepository.findAll();

        for (Gender g : allGenders) {
            String dbName = normalizeBasic(g.getGenderName());
            if (dbName.equals(normInput) ||
                    (normInput.length() > 0 && dbName.startsWith(normInput.substring(0, 1)))) {
                log.info("Gender Input: {} | Database Match: {} | Resolved ID: {}", genderName, g.getGenderName(),
                        g.getId());
                return g.getId();
            }
        }

        List<String> avail = allGenders.stream().map(Gender::getGenderName).collect(Collectors.toList());
        log.warn("Cannot resolve Gender\nInput: {}\nAvailable database values: {}\nResolved ID: NULL", genderName,
                avail);

        Gender newG = genderRepository.save(Gender.builder().genderName(genderName.trim()).build());
        log.info("Auto-created Gender: {} | Resolved ID: {}", newG.getGenderName(), newG.getId());
        return newG.getId();
    }

    public Long resolveAcademicYear(String academicYear) {
        if (academicYear == null || academicYear.trim().isEmpty())
            return null;
        String normInput = normalizeBasic(academicYear);
        List<AcademicYear> allAy = academicYearRepository.findAll();

        for (AcademicYear ay : allAy) {
            if (normalizeBasic(ay.getAcademicYear()).equals(normInput)) {
                log.info("AcademicYear Input: {} | Database Match: {} | Resolved ID: {}", academicYear,
                        ay.getAcademicYear(), ay.getId());
                return ay.getId();
            }
        }

        List<String> avail = allAy.stream().map(AcademicYear::getAcademicYear).collect(Collectors.toList());
        log.warn("Cannot resolve Academic Year\nInput: {}\nAvailable database values: {}\nResolved ID: NULL",
                academicYear, avail);

        String ayTrim = academicYear.trim();
        AcademicYear newAy = academicYearRepository.save(
                AcademicYear.builder().academicYear(ayTrim).startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusYears(1)).status(AcademicYear.Status.ACTIVE).build());
        log.info("Auto-created AcademicYear: {} | Resolved ID: {}", newAy.getAcademicYear(), newAy.getId());
        return newAy.getId();
    }

    public Long resolveYear(String yearStr) {
        if (yearStr == null || yearStr.trim().isEmpty())
            return getDefaultYear();
        String digits = extractDigits(yearStr);
        String lower = yearStr.toLowerCase();

        if (digits.isEmpty()) {
            if (lower.contains("first") || lower.contains("one"))
                digits = "1";
            else if (lower.contains("second") || lower.contains("two"))
                digits = "2";
            else if (lower.contains("third") || lower.contains("three"))
                digits = "3";
            else if (lower.contains("fourth") || lower.contains("four"))
                digits = "4";
        }

        List<Year> allYears = yearRepository.findAll();

        for (Year y : allYears) {
            if (String.valueOf(y.getYearNo()).equals(digits) || extractDigits(y.getYearName()).equals(digits)) {
                log.info("Year Input: {} | Database Match: {} | Resolved ID: {}", yearStr, y.getYearName(), y.getId());
                return y.getId();
            }
        }

        List<String> avail = allYears.stream().map(Year::getYearName).collect(Collectors.toList());
        log.warn("Cannot resolve Year\nInput: {}\nAvailable database values: {}\nResolved ID: NULL", yearStr, avail);

        if (!digits.isEmpty()) {
            byte yNo = Byte.parseByte(digits);
            Year newY = yearRepository.save(Year.builder().yearNo(yNo).yearName(yNo + " Year").build());
            log.info("Auto-created Year: {} | Resolved ID: {}", newY.getYearName(), newY.getId());
            return newY.getId();
        }
        return getDefaultYear();
    }

    public Long resolveSemester(String semStr) {
        if (semStr == null || semStr.trim().isEmpty())
            return getDefaultSemester();
        String digits = extractDigits(semStr);
        String lower = semStr.toLowerCase();

        if (digits.isEmpty()) {
            if (lower.contains("first") || lower.contains("one"))
                digits = "1";
            else if (lower.contains("second") || lower.contains("two"))
                digits = "2";
            else if (lower.contains("third") || lower.contains("three"))
                digits = "3";
            else if (lower.contains("fourth") || lower.contains("four"))
                digits = "4";
            else if (lower.contains("fifth") || lower.contains("five"))
                digits = "5";
            else if (lower.contains("sixth") || lower.contains("six"))
                digits = "6";
            else if (lower.contains("seventh") || lower.contains("seven"))
                digits = "7";
            else if (lower.contains("eighth") || lower.contains("eight"))
                digits = "8";
        }

        List<Semester> allSems = semesterRepository.findAll();

        for (Semester s : allSems) {
            if (String.valueOf(s.getSemesterNo()).equals(digits) || extractDigits(s.getSemesterName()).equals(digits)) {
                log.info("Semester Input: {} | Database Match: {} | Resolved ID: {}", semStr, s.getSemesterName(),
                        s.getId());
                return s.getId();
            }
        }

        List<String> avail = allSems.stream().map(Semester::getSemesterName).collect(Collectors.toList());
        log.warn("Cannot resolve Semester\nInput: {}\nAvailable database values: {}\nResolved ID: NULL", semStr, avail);

        if (!digits.isEmpty()) {
            byte sNo = Byte.parseByte(digits);
            Semester newS = semesterRepository
                    .save(Semester.builder().semesterNo(sNo).semesterName("Semester " + sNo).build());
            log.info("Auto-created Semester: {} | Resolved ID: {}", newS.getSemesterName(), newS.getId());
            return newS.getId();
        }
        return getDefaultSemester();
    }

    public Long resolveSection(String sectionStr, Long departmentId) {
        if (sectionStr == null || sectionStr.trim().isEmpty() || departmentId == null)
            return null;
        Department d = departmentRepository.findById(departmentId).orElse(null);
        if (d != null) {
            String normInput = normalizeBasic(sectionStr);
            String letter = normInput.replace("section", "").replace("sec", "");

            List<Section> allSecs = sectionRepository.findAll().stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(departmentId))
                    .collect(Collectors.toList());

            for (Section sec : allSecs) {
                String dbNorm = normalizeBasic(sec.getSectionName());
                String dbLetter = dbNorm.replace("section", "").replace("sec", "");
                if (letter.equals(dbLetter) && !letter.isEmpty()) {
                    log.info("Section Input: {} | Database Match: {} | Resolved ID: {}", sectionStr,
                            sec.getSectionName(), sec.getId());
                    return sec.getId();
                }
            }

            List<String> avail = allSecs.stream().map(Section::getSectionName).collect(Collectors.toList());
            log.warn(
                    "Cannot resolve Section for Department ID {}\nInput: {}\nAvailable database values: {}\nResolved ID: NULL",
                    departmentId, sectionStr, avail);

            Section newSec = sectionRepository
                    .save(Section.builder().department(d).sectionName(sectionStr.trim()).build());
            log.info("Auto-created Section: {} | Resolved ID: {}", newSec.getSectionName(), newSec.getId());
            return newSec.getId();
        }
        log.warn(
                "Cannot resolve Section\nInput: {}\nAvailable database values: Department not found\nResolved ID: NULL",
                sectionStr);
        return null;
    }

    private Long getDefaultYear() {
        byte fallbackNo = 1;
        Year firstYear = yearRepository.findByYearNo(fallbackNo)
                .orElseGet(() -> yearRepository.save(Year.builder().yearNo(fallbackNo).yearName("1 Year").build()));
        return firstYear.getId();
    }

    private Long getDefaultSemester() {
        byte fallbackNo = 1;
        Semester firstSemester = semesterRepository.findBySemesterNo(fallbackNo)
                .orElseGet(() -> semesterRepository
                        .save(Semester.builder().semesterNo(fallbackNo).semesterName("Semester 1").build()));
        return firstSemester.getId();
    }

    private Long getDefaultGender() {
        return genderRepository.findAll().stream()
                .filter(g -> g.getGenderName().equalsIgnoreCase("Male"))
                .findFirst()
                .orElseGet(() -> genderRepository.save(Gender.builder().genderName("Male").build()))
                .getId();
    }
}
