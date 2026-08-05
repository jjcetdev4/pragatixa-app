package com.pragatix.repository;

import com.pragatix.entity.ActivityCompletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityCompletionRequestRepository extends JpaRepository<ActivityCompletionRequest, Long> {

    @Query("SELECT r FROM ActivityCompletionRequest r WHERE r.student.id = :studentId AND r.activity.id = :activityId ORDER BY r.createdAt DESC")
    List<ActivityCompletionRequest> findByStudentIdAndActivityIdOrderByCreatedAtDesc(@Param("studentId") Long studentId,
            @Param("activityId") Long activityId);

    @Query("SELECT r FROM ActivityCompletionRequest r WHERE r.team.id = :teamId AND r.activity.id = :activityId ORDER BY r.createdAt DESC")
    List<ActivityCompletionRequest> findByTeamIdAndActivityIdOrderByCreatedAtDesc(@Param("teamId") Long teamId,
            @Param("activityId") Long activityId);

    @Query("SELECT r FROM ActivityCompletionRequest r WHERE r.student.id = :studentId OR (r.team IS NOT NULL AND r.team.captain.id = :studentId) ORDER BY r.createdAt DESC")
    List<ActivityCompletionRequest> findMyRequests(@Param("studentId") Long studentId);

    @Query("SELECT r FROM ActivityCompletionRequest r WHERE r.cc.id = :ccId AND (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    List<ActivityCompletionRequest> findByCcIdAndOptionalStatus(@Param("ccId") Long ccId,
            @Param("status") String status);

    @Query("SELECT DISTINCT r FROM ActivityCompletionRequest r " +
            "LEFT JOIN ActivityAssignment aa ON r.activity = aa.activity " +
            "WHERE (r.cc.id = :teacherId " +
            "OR aa.teacher.id = :teacherId " +
            "OR r.activity.subgroup.assignedFaculty.id = :teacherId) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.createdAt DESC")
    List<ActivityCompletionRequest> findPossibleRequestsForTeacher(@Param("teacherId") Long teacherId,
            @Param("status") String status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ActivityCompletionRequest r WHERE r.activity.id = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}
