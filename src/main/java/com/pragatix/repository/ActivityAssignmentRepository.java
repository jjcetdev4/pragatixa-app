package com.pragatix.repository;

import com.pragatix.entity.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findAll();

    long countByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByActivityIdIn(List<Long> activityIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    Optional<ActivityAssignment> findByActivityIdAndSectionId(Long activityId, Long sectionId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    Optional<ActivityAssignment> findByActivityIdAndSectionIsNull(Long activityId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByTeacherId(Long teacherId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByActivityIdAndTeacherId(Long activityId, Long teacherId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.department.id = :departmentId AND a.year = :year AND a.section.id = :sectionId AND (a.teacher IS NULL OR a.teacher.id = :teacherId)")
    List<ActivityAssignment> findByTeacherAndDeptAndYearAndSection(
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("year") String year,
            @org.springframework.data.repository.query.Param("sectionId") Long sectionId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.activity.id = :activityId AND a.department.id = :departmentId AND a.year = :year AND a.section.id = :sectionId AND (a.teacher IS NULL OR a.teacher.id = :teacherId)")
    List<ActivityAssignment> findByActivityIdAndTeacherAndDeptAndYearAndSection(
            @org.springframework.data.repository.query.Param("activityId") Long activityId,
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("year") String year,
            @org.springframework.data.repository.query.Param("sectionId") Long sectionId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.teacher.id = :teacherId OR (a.teacher IS NULL AND a.department.id = :departmentId)")
    List<ActivityAssignment> findMyAndGlobalAssignments(Long teacherId, Long departmentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityAssignment a WHERE a.activity.id = :activityId AND (a.teacher.id = :teacherId OR (a.teacher IS NULL AND a.department.id = :departmentId))")
    List<ActivityAssignment> findByActivityIdAndTeacherIdOrTeacherIsNull(Long activityId, Long teacherId,
            Long departmentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "stage", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByActivityIdAndStageId(Long activityId, Long stageId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "stage", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByActivityIdAndStageIdIsNull(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActivityId(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActivityIdAndStageId(Long activityId, Long stageId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActivityIdAndStageIdIsNull(Long activityId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM ActivityAssignment a WHERE a.activity.id = :activityId AND a.assignmentScope = :scope")
    void deleteByActivityIdAndScope(
            @org.springframework.data.repository.query.Param("activityId") Long activityId,
            @org.springframework.data.repository.query.Param("scope") com.pragatix.entity.AssignmentScope scope);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity", "department", "section",
            "teacher", "assignedBy" })
    List<ActivityAssignment> findByAssignmentScope(com.pragatix.entity.AssignmentScope scope);
}
