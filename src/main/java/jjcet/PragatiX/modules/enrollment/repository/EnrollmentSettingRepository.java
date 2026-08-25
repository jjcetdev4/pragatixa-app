package jjcet.PragatiX.modules.enrollment.repository;

import jjcet.PragatiX.modules.enrollment.entity.EnrollmentSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentSettingRepository extends JpaRepository<EnrollmentSetting, Long> {
}
