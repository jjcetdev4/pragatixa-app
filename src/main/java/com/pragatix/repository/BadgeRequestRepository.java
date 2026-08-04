package com.pragatix.repository;

import com.pragatix.entity.BadgeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRequestRepository extends JpaRepository<BadgeRequest, Long> {

    List<BadgeRequest> findByStudentId(Long studentId);

    List<BadgeRequest> findByStudentIdAndBadgeId(Long studentId, Long badgeId);

    List<BadgeRequest> findByDepartmentIdAndSectionId(Long departmentId, Long sectionId);

    long countByStatus(String status);

    long countByStatusAndDepartmentIdAndSectionId(String status, Long departmentId, Long sectionId);

}
