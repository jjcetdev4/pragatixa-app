package com.spdms.repository;

import com.spdms.entity.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Long> {
    List<StudentBadge> findByStudentId(Long studentId);
    Optional<StudentBadge> findByStudentIdAndBadgeId(Long studentId, Long badgeId);
    boolean existsByStudentIdAndBadgeId(Long studentId, Long badgeId);
    List<StudentBadge> findByStatus(String status);
}
