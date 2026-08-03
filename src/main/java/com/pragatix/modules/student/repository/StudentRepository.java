package com.pragatix.modules.student.repository;

import com.pragatix.entity.Student;
import com.pragatix.entity.Department;
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

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       List<Student> findAll();

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       Page<Student> findAll(Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       Optional<Student> findById(Long id);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       List<Student> findAllById(Iterable<Long> ids);

       Optional<Student> findByEmail(String email);

       @Modifying
       @Transactional
       @Query("UPDATE Student s SET s.stage = :stageOrder")
       void updateAllStudentsCurrentStage(@Param("stageOrder") int stageOrder);

       List<Student> findByActiveTrue();

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       List<Student> findByDepartmentId(Long departmentId);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.section.id = :sectionId")
       List<Student> findByDepartmentIdAndSectionId(@Param("deptId") Long deptId, @Param("sectionId") Long sectionId);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "user" })
       List<Student> findByYearRefIdAndDepartmentIdAndSectionId(Long yearId, Long departmentId, Long sectionId);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "user" })
       List<Student> findByYearRefIdAndDepartmentId(Long yearId, Long departmentId);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "user" })
       List<Student> findByYearRefId(Long yearId);

       @Query("SELECT DISTINCT s.department FROM Student s WHERE s.yearRef.id = :yearId AND s.department IS NOT NULL")
       List<Department> findDistinctDepartmentsByYearId(@Param("yearId") Long yearId);

       long countByDepartmentId(Long departmentId);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       Optional<Student> findByRegNo(String regNo);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       List<Student> findByRegNoIn(List<String> regNos);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       Optional<Student> findBySprNo(String sprNo);

       Optional<Student> findByUserId(Long userId);

       boolean existsByEmail(String email);

       boolean existsByRegNo(String regNo);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.yearRef.id = :yearId AND s.section.id = :sectionId")
       Page<Student> findByDepartmentAndYearAndSection(
                     @Param("deptId") Long deptId,
                     @Param("yearId") Long yearId,
                     @Param("sectionId") Long sectionId,
                     Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.department.id = :deptId AND s.yearRef.id = :yearId AND s.section.id = :sectionId AND ("
                     +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.regNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Student> searchStudentsByCC(
                     @Param("keyword") String keyword,
                     @Param("deptId") Long deptId,
                     @Param("yearId") Long yearId,
                     @Param("sectionId") Long sectionId,
                     Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE " +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.regNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Student> searchStudents(@Param("keyword") String keyword, Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.year = :year")
       Page<Student> findAllByYear(@Param("year") String year, Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.year = :year AND (" +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.regNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Student> searchStudentsByYear(@Param("keyword") String keyword, @Param("year") String year,
                     Pageable pageable);

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "department", "section", "genderRef",
                     "academicYearRef", "yearRef", "semesterRef", "team" })
       @Query("SELECT s FROM Student s WHERE s.active = true AND (" +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.regNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.sprNo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       List<Student> searchActiveStudentsForTeam(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT COUNT(s) FROM Student s WHERE s.active = true AND " +
                     "(s.department.id = :deptId OR (s.department IS NULL AND :deptId IS NULL)) AND " +
                     "(s.year = :year OR (s.year IS NULL AND :year IS NULL)) AND " +
                     "(s.section.id = :secId OR (s.section IS NULL AND :secId IS NULL))")
       long countByDepartmentIdAndYearAndSectionId(@Param("deptId") Long deptId, @Param("year") String year,
                     @Param("secId") Long secId);

       @Query("SELECT COUNT(s) FROM Student s WHERE s.year = :year")
       long countByYear(@Param("year") String year);

       @Query("SELECT COUNT(s) FROM Student s WHERE s.active = true AND s.stage >= :stageOrder AND s.promotionOrder IS NOT NULL AND "
                     +
                     "(s.department.id = :deptId OR (s.department IS NULL AND :deptId IS NULL)) AND " +
                     "(s.year = :year OR (s.year IS NULL AND :year IS NULL)) AND " +
                     "(s.section.id = :secId OR (s.section IS NULL AND :secId IS NULL))")
       Integer countPromotedStudents(@Param("stageOrder") int stageOrder, @Param("deptId") Long deptId,
                     @Param("year") String year, @Param("secId") Long secId);

       @Query("SELECT COUNT(s) FROM Student s WHERE s.active = true AND s.stage >= :stageOrder AND s.promotionOrder IS NOT NULL AND s.id != :studentId AND "
                     +
                     "(s.department.id = :deptId OR (s.department IS NULL AND :deptId IS NULL)) AND " +
                     "(s.year = :year OR (s.year IS NULL AND :year IS NULL)) AND " +
                     "(s.section.id = :secId OR (s.section IS NULL AND :secId IS NULL))")
       Integer countPromotedStudentsExcluding(@Param("stageOrder") int stageOrder, @Param("deptId") Long deptId,
                     @Param("year") String year, @Param("secId") Long secId, @Param("studentId") Long studentId);

       @Query("SELECT COUNT(s) + 1 FROM Student s WHERE s.active = true AND s.totalXp > :xp")
       int getStudentRankByTotalXp(@Param("xp") int xp);
}
