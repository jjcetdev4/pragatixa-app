package com.pragatix.modules.attendance.repository;

import com.pragatix.entity.CaptainRewardSettings;
import com.pragatix.enums.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaptainRewardSettingsRepository extends JpaRepository<CaptainRewardSettings, Long> {
    Optional<CaptainRewardSettings> findByAcademicYear(AcademicYear academicYear);
}
