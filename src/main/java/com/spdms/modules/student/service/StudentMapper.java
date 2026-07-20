package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.modules.activity.dto.request.*;
import com.spdms.modules.activity.dto.response.*;
import com.spdms.modules.student.dto.request.*;
import com.spdms.modules.student.dto.response.*;
import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.activity.repository.*;
import com.spdms.modules.faculty.repository.*;
import com.spdms.modules.student.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;



@Service
public class StudentMapper {
    private static final Logger log = LoggerFactory.getLogger(StudentMapper.class);

    public StudentResponse toResponse(Student student) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null && student.getTeam().getCaptain().getId().equals(student.getId());

        return StudentResponse.builder()
            .id(student.getId())
            .regNo(student.getRegNo())
            .fullName(student.getFullName())
            .email(student.getEmail())
            .phone(student.getPhone())
            .gender(student.getGender())
            .genderId(student.getGenderRef() != null ? student.getGenderRef().getId() : null)
            .dateOfBirth(student.getDateOfBirth())
            .address(student.getAddress())
            .departmentId(student.getDepartment() != null ? student.getDepartment().getId() : null)
            .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
            .semester(student.getSemester())
            .semesterId(student.getSemesterRef() != null ? student.getSemesterRef().getId() : null)
            .academicYear(student.getAcademicYear())
            .academicYearId(student.getAcademicYearRef() != null ? student.getAcademicYearRef().getId() : null)
            .year(student.getYear())
            .yearId(student.getYearRef() != null ? student.getYearRef().getId() : null)
            .section(student.getSection() != null ? student.getSection().getSectionName() : null)
            .sectionId(student.getSection() != null ? student.getSection().getId() : null)
            .sectionName(student.getSection() != null ? student.getSection().getSectionName() : null)
            .active(student.isActive())
            .createdAt(student.getCreatedAt())
            .sprNo(student.getSprNo())
            .score(student.getScore())
            .teamId(teamId)
            .teamName(teamName)
            .isCaptain(isCap)
            .build();
    }

}
