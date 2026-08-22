package jjcet.PragatiX.modules.student.repository;

import jjcet.PragatiX.entity.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Long> {
    List<StudentBadge> findByStudentId(Long studentId);

    List<StudentBadge> findByStudentIdAndBadgeId(Long studentId, Long badgeId);

    boolean existsByStudentIdAndBadgeId(Long studentId, Long badgeId);

    List<StudentBadge> findByStatus(String status);
}
