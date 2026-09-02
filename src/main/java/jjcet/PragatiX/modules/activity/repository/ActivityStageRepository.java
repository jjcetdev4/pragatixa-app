package jjcet.PragatiX.modules.activity.repository;

import jjcet.PragatiX.entity.ActivityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStageRepository extends JpaRepository<ActivityStage, Long> {
    Optional<ActivityStage> findByIdAndDeletedFalse(Long id);

    Optional<ActivityStage> findByName(String name);

    Optional<ActivityStage> findByNameAndDeletedFalse(String name);

    boolean existsByName(String name);

    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndIdNotAndDeletedFalse(String name, Long id);

    List<ActivityStage> findByStatus(jjcet.PragatiX.enums.StageStatus status);

    List<ActivityStage> findByStatusAndDeletedFalse(jjcet.PragatiX.enums.StageStatus status);

    List<ActivityStage> findAllByOrderByDisplayOrderAsc();

    List<ActivityStage> findAllByDeletedFalseOrderByDisplayOrderAsc();

    List<ActivityStage> findByAcademicYearOrderByDisplayOrderAsc(jjcet.PragatiX.enums.AcademicYear academicYear);

    List<ActivityStage> findByAcademicYearAndDeletedFalseOrderByDisplayOrderAsc(jjcet.PragatiX.enums.AcademicYear academicYear);

    List<ActivityStage> findByAcademicYear(jjcet.PragatiX.enums.AcademicYear academicYear);

    List<ActivityStage> findByAcademicYearAndDeletedFalse(jjcet.PragatiX.enums.AcademicYear academicYear);

    Optional<ActivityStage> findByDisplayOrder(int displayOrder);

    Optional<ActivityStage> findByDisplayOrderAndDeletedFalse(int displayOrder);

    Optional<ActivityStage> findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(int displayOrder);

    Optional<ActivityStage> findFirstByDisplayOrderGreaterThanAndDeletedFalseOrderByDisplayOrderAsc(int displayOrder);

    Optional<ActivityStage> findFirstByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<ActivityStage> findFirstByIsActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
}
