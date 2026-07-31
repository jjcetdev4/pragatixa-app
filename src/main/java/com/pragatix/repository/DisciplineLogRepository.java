package com.pragatix.repository;

import com.pragatix.entity.DisciplineLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisciplineLogRepository extends JpaRepository<DisciplineLog, Long> {
    List<DisciplineLog> findByStudentIdOrderByCreatedAtDesc(Long regNo);

    long countByActivityId(Long activityId);

    @Modifying
    @Query("UPDATE DisciplineLog dl SET dl.subgroup = null WHERE dl.subgroup.id = :subgroupId")
    void nullifySubgroupReferences(@Param("subgroupId") Long subgroupId);

    @Modifying
    @Query("UPDATE DisciplineLog dl SET dl.activity = null WHERE dl.activity.id = :activityId")
    void nullifyActivityReferences(@Param("activityId") Long activityId);

    @Query("SELECT COALESCE(SUM(dl.points), 0) FROM DisciplineLog dl WHERE dl.student.id = :regNo AND dl.activity.id = :activityId AND (dl.remarks IS NULL OR dl.remarks NOT IN ('PENDING', 'PENDING REVIEW', 'REJECTED'))")
    int sumApprovedPointsByStudentAndActivity(@Param("regNo") Long regNo, @Param("activityId") Long activityId);
}
