package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
        @Deprecated
        Optional<Team> findByName(String name);

        @Deprecated
        boolean existsByName(String name);

        java.util.List<Team> findByDepartmentIdAndYearAndSectionId(Long departmentId, String year, Long sectionId);

        boolean existsByNameAndDepartmentIdAndYearAndSectionId(String name, Long departmentId, String year,
                        Long sectionId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Team t WHERE LOWER(TRIM(t.name)) = LOWER(TRIM(:name)) AND "
                        +
                        "(t.department.id = :deptId OR (t.department IS NULL AND :deptId IS NULL)) AND " +
                        "(t.year = :year OR (t.year IS NULL AND :year IS NULL)) AND " +
                        "(t.section.id = :secId OR (t.section IS NULL AND :secId IS NULL))")
        boolean existsByTeamNameAndClass(
                        @org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("year") String year,
                        @org.springframework.data.repository.query.Param("secId") Long secId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) > 0 FROM Team t WHERE LOWER(TRIM(t.name)) = LOWER(TRIM(:name)) AND "
                        +
                        "(t.department.id = :deptId OR (t.department IS NULL AND :deptId IS NULL)) AND " +
                        "(t.year = :year OR (t.year IS NULL AND :year IS NULL)) AND " +
                        "(t.section.id = :secId OR (t.section IS NULL AND :secId IS NULL)) AND " +
                        "(:excludeTeamId IS NULL OR t.id <> :excludeTeamId)")
        boolean existsByTeamNameAndClassExcludingId(
                        @org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("year") String year,
                        @org.springframework.data.repository.query.Param("secId") Long secId,
                        @org.springframework.data.repository.query.Param("excludeTeamId") Long excludeTeamId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.captain LEFT JOIN FETCH t.viceCaptain LEFT JOIN FETCH t.department LEFT JOIN FETCH t.section")
        java.util.List<Team> findAllWithMembers();

        @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.captain LEFT JOIN FETCH t.department LEFT JOIN FETCH t.section WHERE t.id = :id")
        Optional<Team> findByIdWithMembers(@org.springframework.data.repository.query.Param("id") Long id);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Team t " +
                        "LEFT JOIN t.members m " +
                        "LEFT JOIN StageTeam st ON st.team = t " +
                        "WHERE (m.id = :studentId) OR (t.captain.id = :studentId) OR (t.viceCaptain.id = :studentId) OR (st.captain.id = :studentId) OR (st.viceCaptain.id = :studentId)")
        java.util.List<Team> findAllTeamsByStudentId(
                        @org.springframework.data.repository.query.Param("studentId") Long studentId);

        default Optional<Team> findTeamByStudentId(Long studentId) {
                java.util.List<Team> teams = findAllTeamsByStudentId(studentId);
                if (teams == null || teams.isEmpty()) {
                        return Optional.empty();
                }
                if (teams.size() > 1) {
                        String teamDetails = teams.stream()
                                        .map(t -> String.format("Team[id=%d, name='%s']", t.getId(), t.getName()))
                                        .collect(java.util.stream.Collectors.joining(", "));
                        org.slf4j.LoggerFactory.getLogger(TeamRepository.class).error(
                                        "DATABASE INTEGRITY VIOLATION: Student ID {} is associated with multiple teams: [{}]",
                                        studentId, teamDetails);
                        throw new IllegalStateException(
                                        "Database integrity violation: Student ID " + studentId
                                                        + " is associated with multiple teams: " + teamDetails);
                }
                return Optional.of(teams.get(0));
        }

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
