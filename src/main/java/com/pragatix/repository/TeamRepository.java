package com.pragatix.repository;

import com.pragatix.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
        Optional<Team> findByName(String name);

        boolean existsByName(String name);

        java.util.List<Team> findByDepartmentIdAndYearAndSectionId(Long departmentId, String year, Long sectionId);

        boolean existsByNameAndDepartmentIdAndYearAndSectionId(String name, Long departmentId, String year,
                        Long sectionId);

        @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.captain LEFT JOIN FETCH t.department LEFT JOIN FETCH t.section WHERE t.id = :id")
        Optional<Team> findByIdWithMembers(@org.springframework.data.repository.query.Param("id") Long id);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Team t " +
                        "LEFT JOIN t.members m " +
                        "LEFT JOIN StageTeam st ON st.team = t " +
                        "WHERE (m.id = :studentId) OR (t.captain.id = :studentId) OR (st.viceCaptain.id = :studentId)")
        Optional<Team> findTeamByStudentId(
                        @org.springframework.data.repository.query.Param("studentId") Long studentId);

        @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t WHERE t.name = :name AND " +
                        "(t.department.id = :deptId OR (t.department IS NULL AND :deptId IS NULL)) AND " +
                        "(t.year = :year OR (t.year IS NULL AND :year IS NULL)) AND " +
                        "(t.section.id = :secId OR (t.section IS NULL AND :secId IS NULL))")
        Optional<Team> findExactTeam(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("secId") Long secId,
                        @org.springframework.data.repository.query.Param("year") String year);

        @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t " +
                        "WHERE (:academicYear IS NULL OR t.year = :academicYear) " +
                        "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
                        "AND (:sectionId IS NULL OR t.section.id = :sectionId)")
        java.util.List<Team> findFilteredTeams(
                        @org.springframework.data.repository.query.Param("academicYear") String academicYear,
                        @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
                        @org.springframework.data.repository.query.Param("sectionId") Long sectionId);
}
