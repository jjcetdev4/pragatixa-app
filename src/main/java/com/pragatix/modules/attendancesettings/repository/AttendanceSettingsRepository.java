package jjcet.PragatiX.modules.attendancesettings.repository;

import jjcet.PragatiX.entity.AttendanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jjcet.PragatiX.enums.AcademicYear;
import java.util.Optional;

@Repository
public interface AttendanceSettingsRepository extends JpaRepository<AttendanceSettings, Long> {
    Optional<AttendanceSettings> findByAcademicYear(AcademicYear academicYear);
}
