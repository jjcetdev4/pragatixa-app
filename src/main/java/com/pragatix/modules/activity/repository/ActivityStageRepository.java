package com.pragatix.modules.activity.repository;

import com.pragatix.entity.ActivityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStageRepository extends JpaRepository<ActivityStage, Long> {
    Optional<ActivityStage> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<ActivityStage> findByStatus(com.pragatix.enums.StageStatus status);

    List<ActivityStage> findAllByOrderByDisplayOrderAsc();

    List<ActivityStage> findByAcademicYearOrderByDisplayOrderAsc(com.pragatix.enums.AcademicYear academicYear);

    List<ActivityStage> findByAcademicYear(com.pragatix.enums.AcademicYear academicYear);

    Optional<ActivityStage> findByDisplayOrder(int displayOrder);

    Optional<ActivityStage> findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(int displayOrder);

    Optional<ActivityStage> findFirstByIsActiveTrueOrderByDisplayOrderAsc();
}
