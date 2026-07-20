package com.spdms.modules.student.repository;

import com.spdms.entity.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Long> {
    List<StudentBadge> findByStudentId(Long studentId);
    List<StudentBadge> findByStudentIdAndBadgeId(Long regNo, Long badgeId);
    boolean existsByStudentIdAndBadgeId(Long regNo, Long badgeId);
    List<StudentBadge> findByStatus(String status);
}
