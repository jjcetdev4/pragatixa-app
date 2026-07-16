package com.spdms.modules.student.repository;

import com.spdms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Student s SET s.totalXp = :totalXp, s.score = :score WHERE s.id = :studentId")
    void updateStudentXpAndScore(@Param("studentId") Long studentId, @Param("totalXp") int totalXp, @Param("score") int score);

    Optional<Student> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE Student s SET s.currentStage = :stageOrder")
    void updateAllStudentsCurrentStage(@Param("stageOrder") int stageOrder);

    List<Student> findByActiveTrue();

    List<Student> findByDepartmentId(Long departmentId);
    
    @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.section.id = :sectionId")
    List<Student> findByDepartmentIdAndSectionId(@Param("deptId") Long deptId, @Param("sectionId") Long sectionId);
    
    long countByDepartmentId(Long departmentId);

    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByRegNo(Long regNo);
    Optional<Student> findBySprNo(String sprNo);

    Optional<Student> findByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.yearRef.id = :yearId AND s.section.id = :sectionId")
    Page<Student> findByDepartmentAndYearAndSection(
        @Param("deptId") Long deptId,
        @Param("yearId") Long yearId,
        @Param("sectionId") Long sectionId,
        Pageable pageable
    );

    @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.yearRef.id = :yearId AND s.section.id = :sectionId AND (" +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Student> searchStudentsByCC(
        @Param("keyword") String keyword,
        @Param("deptId") Long deptId,
        @Param("yearId") Long yearId,
        @Param("sectionId") Long sectionId,
        Pageable pageable
    );

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Student> searchStudents(@Param("keyword") String keyword, Pageable pageable);
}
