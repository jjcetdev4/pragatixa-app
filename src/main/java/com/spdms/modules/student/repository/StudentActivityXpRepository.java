package com.spdms.modules.student.repository;

import com.spdms.entity.StudentActivityXp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentActivityXpRepository extends JpaRepository<StudentActivityXp, Long> {
    List<StudentActivityXp> findByStudentId(Long studentId);
    List<StudentActivityXp> findByStudentIdAndActivityId(Long regNo, Long activityId);
    
    long countByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByAssignmentId(Long assignmentId);

    boolean existsByAssignmentAndStudentIn(com.spdms.entity.ActivityAssignment assignment, java.util.Collection<com.spdms.entity.Student> students);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(x.xpAwarded) FROM StudentActivityXp x WHERE x.student.id = :studentId AND x.activity.subgroup.id = :subgroupId AND x.result != 'FAIL'")
    Integer calculateXpBySubgroup(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("subgroupId") Long subgroupId);
}
