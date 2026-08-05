package com.pragatix.repository;

import com.pragatix.entity.PenaltyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRequestRepository extends JpaRepository<PenaltyRequest, Long> {

    @Query("SELECT p FROM PenaltyRequest p WHERE p.cc.id = :ccId AND (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<PenaltyRequest> findByCcIdAndOptionalStatus(@Param("ccId") Long ccId, @Param("status") String status);

    @Query("SELECT p FROM PenaltyRequest p WHERE p.teacher.id = :teacherId ORDER BY p.createdAt DESC")
    List<PenaltyRequest> findByTeacherId(@Param("teacherId") Long teacherId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PenaltyRequest p WHERE p.activity.id = :activityId")
    int deleteByActivityId(@Param("activityId") Long activityId);

}

