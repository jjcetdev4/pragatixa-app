package com.spdms.repository;

import com.spdms.entity.DisciplineLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisciplineLogRepository extends JpaRepository<DisciplineLog, Long> {
    List<DisciplineLog> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
