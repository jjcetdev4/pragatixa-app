package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.BadgeRequest;
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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) FROM BadgeRequest b WHERE b.status = :status AND b.department.id = :departmentId AND (:sectionId IS NULL OR b.section.id = :sectionId)")
    long countByStatusAndDepartmentIdAndSectionId(@org.springframework.data.repository.query.Param("status") String status, @org.springframework.data.repository.query.Param("departmentId") Long departmentId, @org.springframework.data.repository.query.Param("sectionId") Long sectionId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM BadgeRequest b WHERE b.department.id = :departmentId AND (:sectionId IS NULL OR b.section.id = :sectionId) AND b.student.year = :year")
    List<BadgeRequest> findByDepartmentIdAndSectionIdAndYear(@org.springframework.data.repository.query.Param("departmentId") Long departmentId, @org.springframework.data.repository.query.Param("sectionId") Long sectionId, @org.springframework.data.repository.query.Param("year") String year);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) FROM BadgeRequest b WHERE b.status = :status AND b.department.id = :departmentId AND (:sectionId IS NULL OR b.section.id = :sectionId) AND b.student.year = :year")
    long countByStatusAndDepartmentIdAndSectionIdAndYear(@org.springframework.data.repository.query.Param("status") String status, @org.springframework.data.repository.query.Param("departmentId") Long departmentId, @org.springframework.data.repository.query.Param("sectionId") Long sectionId, @org.springframework.data.repository.query.Param("year") String year);

}
