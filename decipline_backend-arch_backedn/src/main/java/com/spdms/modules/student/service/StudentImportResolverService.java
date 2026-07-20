package com.spdms.modules.student.service;

import com.spdms.entity.*;
import com.spdms.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class StudentImportResolverService {

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

    public Long resolveDepartment(String deptName) {
        if (deptName == null || deptName.trim().isEmpty()) return null;
        String trimmedDept = deptName.trim();
        String code = trimmedDept.length() > 6 ? trimmedDept.substring(0, 4).toUpperCase() : trimmedDept.toUpperCase();

        Department d = departmentRepository.findByDeptCode(code)
                .or(() -> departmentRepository.findByCode(code))
                .or(() -> departmentRepository.findByName(trimmedDept))
                .orElseGet(() -> departmentRepository.save(
                        Department.builder().deptCode(code).deptName(trimmedDept).code(code).name(trimmedDept).description("Auto-created during bulk import").build()
                ));
        return d.getId();
    }

    public Long resolveGender(String genderName) {
        if (genderName == null || genderName.trim().isEmpty()) {
            return genderRepository.findAll().stream()
                    .filter(g -> g.getGenderName().equalsIgnoreCase("Male"))
                    .findFirst()
                    .orElseGet(() -> genderRepository.save(Gender.builder().genderName("Male").build()))
                    .getId();
        }
        String genderTrim = genderName.trim();
        Gender g = genderRepository.findByGenderName(genderTrim)
                .orElseGet(() -> genderRepository.save(Gender.builder().genderName(genderTrim).build()));
        return g.getId();
    }

    public Long resolveAcademicYear(String academicYear) {
        if (academicYear == null || academicYear.trim().isEmpty()) return null;
        String ayTrim = studentLookupService.normalizeAcademicYear(academicYear);
        AcademicYear ay = academicYearRepository.findByAcademicYear(ayTrim)
                .orElseGet(() -> academicYearRepository.save(
                        AcademicYear.builder().academicYear(ayTrim).startDate(LocalDate.now()).endDate(LocalDate.now().plusYears(1)).status(AcademicYear.Status.ACTIVE).build()
                ));
        return ay.getId();
    }

    public Long resolveYear(String yearStr) {
        if (yearStr == null || yearStr.trim().isEmpty()) return getDefaultYear();
        try {
            byte yNo = Byte.parseByte(yearStr.trim());
            Year y = yearRepository.findByYearNo(yNo)
                    .orElseGet(() -> yearRepository.save(Year.builder().yearNo(yNo).yearName(yNo + " Year").build()));
            return y.getId();
        } catch (Exception e) {
            byte fallbackVal = 1;
            String yrLower = yearStr.toLowerCase();
            if (yrLower.contains("2") || yrLower.contains("second")) fallbackVal = 2;
            else if (yrLower.contains("3") || yrLower.contains("third")) fallbackVal = 3;
            else if (yrLower.contains("4") || yrLower.contains("fourth")) fallbackVal = 4;
            final byte finalFallback = fallbackVal;
            Year y = yearRepository.findByYearNo(finalFallback)
                    .orElseGet(() -> yearRepository.save(Year.builder().yearNo(finalFallback).yearName(finalFallback + " Year").build()));
            return y.getId();
        }
    }

    public Long resolveSemester(String semStr) {
        if (semStr == null || semStr.trim().isEmpty()) return getDefaultSemester();
        try {
            byte sNo = Byte.parseByte(semStr.trim());
            Semester s = semesterRepository.findBySemesterNo(sNo)
                    .orElseGet(() -> semesterRepository.save(Semester.builder().semesterNo(sNo).semesterName("Semester " + sNo).build()));
            return s.getId();
        } catch (Exception e) {
            byte fallbackSem = 1;
            for (byte i = 1; i <= 8; i++) {
                if (semStr.contains(String.valueOf(i))) {
                    fallbackSem = i;
                    break;
                }
            }
            final byte finalFallbackSem = fallbackSem;
            Semester s = semesterRepository.findBySemesterNo(finalFallbackSem)
                    .orElseGet(() -> semesterRepository.save(Semester.builder().semesterNo(finalFallbackSem).semesterName("Semester " + finalFallbackSem).build()));
            return s.getId();
        }
    }

    public Long resolveSection(String sectionStr, Long departmentId) {
        if (sectionStr == null || sectionStr.trim().isEmpty() || departmentId == null) return null;
        Department d = departmentRepository.findById(departmentId).orElse(null);
        if (d != null) {
            String secTrim = sectionStr.trim();
            Section sec = sectionRepository.findByDepartmentAndSectionName(d, secTrim)
                    .orElseGet(() -> sectionRepository.save(Section.builder().department(d).sectionName(secTrim).build()));
            return sec.getId();
        }
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
                .orElseGet(() -> semesterRepository.save(Semester.builder().semesterNo(fallbackNo).semesterName("Semester 1").build()));
        return firstSemester.getId();
    }
}
