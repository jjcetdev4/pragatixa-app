package com.spdms.modules.activity.repository;

import com.spdms.entity.ActivityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.spdms.entity.AssignedAcademicYear;

@Repository
public interface ActivityStageRepository extends JpaRepository<ActivityStage, Long> {
    Optional<ActivityStage> findByNameAndAssignedAcademicYear(String name, AssignedAcademicYear assignedAcademicYear);
    boolean existsByNameAndAssignedAcademicYear(String name, AssignedAcademicYear assignedAcademicYear);
    boolean existsByNameAndIdNotAndAssignedAcademicYear(String name, Long id, AssignedAcademicYear assignedAcademicYear);
    List<ActivityStage> findByStatusAndAssignedAcademicYear(com.spdms.enums.StageStatus status, AssignedAcademicYear assignedAcademicYear);
    List<ActivityStage> findAllByAssignedAcademicYearOrderByDisplayOrderAsc(AssignedAcademicYear assignedAcademicYear);
    Optional<ActivityStage> findByDisplayOrderAndAssignedAcademicYear(int displayOrder, AssignedAcademicYear assignedAcademicYear);
    Optional<ActivityStage> findFirstByDisplayOrderGreaterThanAndAssignedAcademicYearOrderByDisplayOrderAsc(int displayOrder, AssignedAcademicYear assignedAcademicYear);
    List<ActivityStage> findAllByOrderByDisplayOrderAsc(); // Still needed for scheduler if it iterates across all
}
