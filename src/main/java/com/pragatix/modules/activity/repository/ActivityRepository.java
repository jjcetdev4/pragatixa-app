package com.pragatix.modules.activity.repository;

import com.pragatix.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByStageId(Long stageId);

    List<Activity> findByStreakEnabledTrue();

    List<Activity> findBySubgroupId(Long subgroupId);

    List<Activity> findBySubgroupIdIn(List<Long> subgroupIds);

    List<Activity> findByActivityName(String activityName);

    List<Activity> findByAcademicYear(com.pragatix.enums.AcademicYear academicYear);

    List<Activity> findByStageIdAndAcademicYear(Long stageId, com.pragatix.enums.AcademicYear academicYear);

    List<Activity> findBySubgroupIdAndAcademicYear(Long subgroupId, com.pragatix.enums.AcademicYear academicYear);

    java.util.Optional<Activity> findByAcademicYearAndAttendanceEngineEnabledTrue(com.pragatix.enums.AcademicYear academicYear);

    java.util.Optional<Activity> findByStageIdAndAttendanceEngineEnabledTrue(Long stageId);

    long countByStageIdAndAttendanceEngineEnabledTrue(Long stageId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT act FROM Activity act JOIN ActivityAssignment a ON a.activity.id = act.id "
            +
            "WHERE a.department.id = :departmentId AND a.year = :year AND a.section.id = :sectionId AND (a.teacher IS NULL OR a.teacher.id = :teacherId)")
    List<Activity> findDistinctActivitiesForCC(
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("year") String year,
            @org.springframework.data.repository.query.Param("sectionId") Long sectionId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT act FROM Activity act JOIN ActivityAssignment a ON a.activity.id = act.id "
            +
            "WHERE a.teacher.id = :teacherId OR (a.teacher IS NULL AND a.department.id = :departmentId)")
    List<Activity> findDistinctActivitiesForDept(
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT act FROM Activity act JOIN ActivityAssignment a ON a.activity.id = act.id "
            +
            "WHERE a.teacher.id = :teacherId")
    List<Activity> findDistinctActivitiesForTeacher(
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId);
}
