package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.StudentActivityStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentActivityStreakRepository extends JpaRepository<StudentActivityStreak, Long> {
    void deleteByActivityId(Long activityId);

    Optional<StudentActivityStreak> findByStudentIdAndActivityId(Long studentId, Long activityId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "activity" })
    List<StudentActivityStreak> findByStudentId(Long studentId);
}
