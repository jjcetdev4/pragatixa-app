package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.ActivityTemporaryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityTemporaryAssignmentRepository extends JpaRepository<ActivityTemporaryAssignment, Long> {

        List<ActivityTemporaryAssignment> findByActivityId(Long activityId);

        List<ActivityTemporaryAssignment> findByActivityIdAndStatus(Long activityId, String status);

        List<ActivityTemporaryAssignment> findByStatusAndExpiryDateLessThan(String status, LocalDate expiryDate);

        List<ActivityTemporaryAssignment> findByTemporaryTeacherIdAndStatus(Long teacherId, String status);

        @Query("SELECT ata FROM ActivityTemporaryAssignment ata WHERE ata.activity.id = :activityId " +
                        "AND ata.status = 'ACTIVE' " +
                        "AND ata.assignmentDate <= :date AND ata.expiryDate >= :date " +
                        "AND (:departmentId IS NULL OR ata.department.id = :departmentId) " +
                        "AND (:sectionId IS NULL OR ata.section.id = :sectionId)")
        List<ActivityTemporaryAssignment> findActiveAssignments(
                        @Param("activityId") Long activityId,
                        @Param("departmentId") Long departmentId,
                        @Param("sectionId") Long sectionId,
                        @Param("date") LocalDate date);

        @Query("SELECT ata FROM ActivityTemporaryAssignment ata WHERE ata.activity.id = :activityId " +
                        "AND ata.status = 'ACTIVE' " +
                        "AND ata.assignmentDate <= :date AND ata.expiryDate >= :date")
        List<ActivityTemporaryAssignment> findActiveByActivityIdAndDate(
                        @Param("activityId") Long activityId,
                        @Param("date") LocalDate date);

        @Query("SELECT ata FROM ActivityTemporaryAssignment ata WHERE ata.temporaryTeacher.id = :teacherId " +
                        "AND ata.status = 'ACTIVE' " +
                        "AND ata.assignmentDate <= :date AND ata.expiryDate >= :date")
        List<ActivityTemporaryAssignment> findActiveByTeacherIdAndDate(
                        @Param("teacherId") Long teacherId,
                        @Param("date") LocalDate date);

        @Query("SELECT DISTINCT ata.activity.id FROM ActivityTemporaryAssignment ata " +
                        "WHERE ata.temporaryTeacher IS NOT NULL " +
                        "AND ata.status = 'ACTIVE' " +
                        "AND ata.assignmentDate <= :date AND ata.expiryDate >= :date " +
                        "AND (:stageId IS NULL OR ata.stage.id = :stageId OR ata.stage IS NULL) " +
                        "AND (:departmentId IS NULL OR ata.department IS NULL OR ata.department.id = :departmentId) " +
                        "AND (:sectionId IS NULL OR ata.section IS NULL OR ata.section.id = :sectionId)")
        List<Long> findActivityIdsWithActiveTemporaryTeacher(
                        @Param("stageId") Long stageId,
                        @Param("departmentId") Long departmentId,
                        @Param("sectionId") Long sectionId,
                        @Param("date") LocalDate date);

        @Query("SELECT DISTINCT ata.activity.id FROM ActivityTemporaryAssignment ata " +
                        "WHERE ata.temporaryTeacher.id = :teacherId " +
                        "AND ata.status = 'ACTIVE' " +
                        "AND ata.assignmentDate <= :date AND ata.expiryDate >= :date " +
                        "AND (:stageId IS NULL OR ata.stage.id = :stageId OR ata.stage IS NULL)")
        List<Long> findActivityIdsByTemporaryTeacher(
                        @Param("teacherId") Long teacherId,
                        @Param("stageId") Long stageId,
                        @Param("date") LocalDate date);
}
