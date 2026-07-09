package com.spdms.repository;

import com.spdms.entity.StudentActivityXp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentActivityXpRepository extends JpaRepository<StudentActivityXp, Long> {
    List<StudentActivityXp> findByStudentIdAndActivityId(Long studentId, Long activityId);
}
