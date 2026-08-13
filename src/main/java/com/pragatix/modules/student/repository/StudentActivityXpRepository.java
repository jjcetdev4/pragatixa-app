package jjcet.PragatiX.modules.student.repository;

import jjcet.PragatiX.entity.StudentActivityXp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentActivityXpRepository extends JpaRepository<StudentActivityXp, Long> {
        List<StudentActivityXp> findByStudentId(Long studentId);

        List<StudentActivityXp> findByStudentIdAndActivityId(Long regNo, Long activityId);

        List<StudentActivityXp> findByStudentIdAndActivityIdAndStage(Long studentId, Long activityId, Integer stage);

        long countByActivityId(Long activityId);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        void deleteByActivityId(Long activityId);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        void deleteByAssignmentId(Long assignmentId);

        boolean existsByAssignmentId(Long assignmentId);

        boolean existsByAssignmentAndStudentIn(jjcet.PragatiX.entity.ActivityAssignment assignment,
                        java.util.Collection<jjcet.PragatiX.entity.Student> students);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(x.xpAwarded) FROM StudentActivityXp x WHERE x.student.id = :studentId AND x.activity.subgroup.id = :subgroupId AND x.result != 'FAIL'")
        Integer calculateXpBySubgroup(@org.springframework.data.repository.query.Param("studentId") Long studentId,
                        @org.springframework.data.repository.query.Param("subgroupId") Long subgroupId);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(x.xpAwarded) FROM StudentActivityXp x WHERE x.student.id = :studentId AND x.activity.subgroup.id = :subgroupId AND x.result != 'FAIL' AND x.stage = :stage")
        Integer calculateXpBySubgroupAndStage(
                        @org.springframework.data.repository.query.Param("studentId") Long studentId,
                        @org.springframework.data.repository.query.Param("subgroupId") Long subgroupId,
                        @org.springframework.data.repository.query.Param("stage") Integer stage);
}
