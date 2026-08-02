package com.pragatix.modules.attendancesettings.repository;

import com.pragatix.entity.AttendanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pragatix.enums.AcademicYear;
import java.util.Optional;

@Repository
public interface AttendanceSettingsRepository extends JpaRepository<AttendanceSettings, Long> {
    Optional<AttendanceSettings> findByAcademicYear(AcademicYear academicYear);
}
