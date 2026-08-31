package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Level;
import jjcet.PragatiX.enums.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {
    Optional<Level> findByLevelNumber(int levelNumber);

    List<Level> findAllByOrderByXpMinAsc();

    List<Level> findByDeletedFalseOrderByXpMinAsc();

    List<Level> findByAcademicYearAndDeletedFalseOrderByXpMinAsc(AcademicYear academicYear);

    Optional<Level> findByIdAndDeletedFalse(Long id);

    Optional<Level> findByLevelNumberAndAcademicYearAndDeletedFalse(int levelNumber, AcademicYear academicYear);

    List<Level> findByDeletedTrue();
}
