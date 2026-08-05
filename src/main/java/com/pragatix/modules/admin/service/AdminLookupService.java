package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.repository.SectionRepository;
import com.pragatix.repository.AcademicYearRepository;
import com.pragatix.repository.YearRepository;
import com.pragatix.repository.SemesterRepository;
import com.pragatix.repository.GenderRepository;
import com.pragatix.entity.AcademicYear;
import com.pragatix.entity.Year;
import com.pragatix.entity.Semester;
import com.pragatix.entity.Gender;
import com.pragatix.entity.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminLookupService {
    private static final Logger log = LoggerFactory.getLogger(AdminLookupService.class);

    private final AcademicYearRepository academicYearRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final GenderRepository genderRepository;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final YearRepository yearRepository;

    public AdminLookupService(AcademicYearRepository academicYearRepository,
            ActivityAssignmentRepository activityAssignmentRepository, GenderRepository genderRepository,
            SectionRepository sectionRepository, SemesterRepository semesterRepository, YearRepository yearRepository) {
        this.academicYearRepository = academicYearRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.genderRepository = genderRepository;
        this.sectionRepository = sectionRepository;
        this.semesterRepository = semesterRepository;
        this.yearRepository = yearRepository;
    }

    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAllAcademicYears() {
        return ResponseEntity
                .ok(ApiResponse.ok("Academic years fetched successfully", academicYearRepository.findAll()));
    }

    public ResponseEntity<ApiResponse<List<Year>>> getAllYears() {
        return ResponseEntity.ok(ApiResponse.ok("Years fetched successfully", yearRepository.findAll()));
    }

    public ResponseEntity<ApiResponse<List<Semester>>> getAllSemesters() {
        return ResponseEntity.ok(ApiResponse.ok("Semesters fetched successfully", semesterRepository.findAll()));
    }

    public ResponseEntity<ApiResponse<List<Gender>>> getAllGenders() {
        // Only return clean gender values: Male, Female, Other
        List<String> validGenders = java.util.Arrays.asList("Male", "Female", "Other");
        List<Gender> filtered = genderRepository.findAll().stream()
                .filter(g -> validGenders.stream()
                        .anyMatch(valid -> valid.equalsIgnoreCase(g.getGenderName())))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Genders fetched successfully", filtered));
    }

    public ResponseEntity<ApiResponse<List<Section>>> getAllSections(Long departmentId) {
        if (departmentId == null) {
            return ResponseEntity.ok(ApiResponse.ok("Sections fetched successfully", sectionRepository.findAll()));
        }
        return ResponseEntity.ok(ApiResponse.ok("Sections fetched successfully", sectionRepository.findByDepartment_IdOrderBySectionNameAsc(departmentId)));
    }

    public static String normalizeYearToRoman(String yr) {
        if (yr == null)
            return null;
        String t = yr.trim().toUpperCase();
        if (t.equals("1") || t.equals("I"))
            return "I";
        if (t.equals("2") || t.equals("II"))
            return "II";
        if (t.equals("3") || t.equals("III"))
            return "III";
        if (t.equals("4") || t.equals("IV"))
            return "IV";
        return yr;
    }

    public void migrateLegacyNullYears() {
        log.debug("Starting default year migration for ActivityAssignment records...");
        List<ActivityAssignment> assignments = activityAssignmentRepository.findAll();
        boolean changed = false;
        for (ActivityAssignment aa : assignments) {
            if (aa.getYear() == null || aa.getYear().trim().isEmpty()) {
                aa.setYear("1");
                activityAssignmentRepository.save(aa);
                changed = true;
            }
        }
        if (changed) {
            log.debug("Default year migration completed successfully.");
        } else {
            log.debug("No legacy ActivityAssignment records needed migration.");
        }
    }

}
