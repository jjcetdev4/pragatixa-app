package com.spdms.repository;

import com.spdms.entity.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Long> {
    List<ActivityAssignment> findByActivityId(Long activityId);
    List<ActivityAssignment> findByActivityIdIn(List<Long> activityIds);
    Optional<ActivityAssignment> findByActivityIdAndSectionId(Long activityId, Long sectionId);
    Optional<ActivityAssignment> findByActivityIdAndSectionIsNull(Long activityId);
    List<ActivityAssignment> findByTeacherId(Long teacherId);
    List<ActivityAssignment> findByActivityIdAndTeacherId(Long activityId, Long teacherId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.department.id = :departmentId AND a.year = :year AND a.section.id = :sectionId AND (a.teacher IS NULL OR a.teacher.id = :teacherId)")
    List<ActivityAssignment> findByTeacherAndDeptAndYearAndSection(
        @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
        @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
        @org.springframework.data.repository.query.Param("year") String year,
        @org.springframework.data.repository.query.Param("sectionId") Long sectionId
    );

    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.activity.id = :activityId AND a.department.id = :departmentId AND a.year = :year AND a.section.id = :sectionId AND (a.teacher IS NULL OR a.teacher.id = :teacherId)")
    List<ActivityAssignment> findByActivityIdAndTeacherAndDeptAndYearAndSection(
        @org.springframework.data.repository.query.Param("activityId") Long activityId,
        @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
        @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
        @org.springframework.data.repository.query.Param("year") String year,
        @org.springframework.data.repository.query.Param("sectionId") Long sectionId
    );

    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.teacher.id = :teacherId OR (a.teacher IS NULL AND a.department.id = :departmentId)")
    List<ActivityAssignment> findMyAndGlobalAssignments(Long teacherId, Long departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.activity.id = :activityId AND (a.teacher.id = :teacherId OR (a.teacher IS NULL AND a.department.id = :departmentId))")
    List<ActivityAssignment> findByActivityIdAndTeacherIdOrTeacherIsNull(Long activityId, Long teacherId, Long departmentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM ActivityAssignment a WHERE a.activity.id = :activityId AND a.assignmentScope = :scope")
    void deleteByActivityIdAndScope(
        @org.springframework.data.repository.query.Param("activityId") Long activityId,
        @org.springframework.data.repository.query.Param("scope") com.spdms.entity.AssignmentScope scope
    );

    List<ActivityAssignment> findByAssignmentScope(com.spdms.entity.AssignmentScope scope);
}
