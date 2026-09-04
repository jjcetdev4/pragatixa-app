package jjcet.PragatiX.modules.authentication.repository;

import jjcet.PragatiX.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.deleted = false")
        java.util.List<User> findAll();

        boolean existsByUsername(String username);
        boolean existsByUsernameAndIdNot(String username, Long id);

        boolean existsByEmail(String email);
        boolean existsByEmailAndIdNot(String email, Long id);

        long countByDepartmentId(Long departmentId);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false")
        java.util.List<User> findByRoleName(
                        @org.springframework.data.repository.query.Param("roleName") String roleName);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE u.assignedYear.id = :yearId AND r.name = :roleName AND u.deleted = false AND u.active = true")
        boolean existsByAssignedYearIdAndRolesName(
                        @org.springframework.data.repository.query.Param("yearId") Long yearId,
                        @org.springframework.data.repository.query.Param("roleName") String roleName);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE u.assignedYear.id = :yearId AND r.name = :roleName AND u.id != :adminId AND u.deleted = false AND u.active = true")
        boolean existsByAssignedYearIdAndRolesNameAndIdNot(
                        @org.springframework.data.repository.query.Param("yearId") Long yearId,
                        @org.springframework.data.repository.query.Param("roleName") String roleName,
                        @org.springframework.data.repository.query.Param("adminId") Long adminId);

        /**
         * Find the Class Coordinator (Teacher with CC sub-role) assigned to a given
         * section.
         */
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
                        "JOIN u.roles r " +
                        "JOIN u.subRoles sr " +
                        "WHERE u.section.id = :sectionId " +
                        "AND u.department.id = :departmentId " +
                        "AND r.name = 'ROLE_TEACHER' " +
                        "AND UPPER(sr.name) = 'CC' " +
                        "AND u.active = true")
        java.util.List<User> findClassCoordinatorsByDepartmentAndSection(
                        @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
                        @org.springframework.data.repository.query.Param("sectionId") Long sectionId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
                        "JOIN u.roles r " +
                        "JOIN u.subRoles sr " +
                        "WHERE u.department.id = :departmentId " +
                        "AND r.name = 'ROLE_TEACHER' " +
                        "AND UPPER(sr.name) = 'CC' " +
                        "AND u.active = true")
        java.util.List<User> findClassCoordinatorsByDepartment(
                        @org.springframework.data.repository.query.Param("departmentId") Long departmentId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
                        "LEFT JOIN u.roles r " +
                        "LEFT JOIN u.subRoles sr " +
                        "WHERE u.department.id = :departmentId " +
                        "AND (r.name = 'ROLE_HOD' OR UPPER(sr.name) = 'HOD' OR UPPER(sr.name) = 'HEAD_OF_DEPARTMENT') " +
                        "AND u.active = true")
        java.util.List<User> findHODByDepartment(
                        @org.springframework.data.repository.query.Param("departmentId") Long departmentId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
                        "JOIN u.roles r " +
                        "JOIN u.subRoles sr " +
                        "WHERE r.name = 'ROLE_TEACHER' " +
                        "AND UPPER(sr.name) = 'CC' " +
                        "AND u.active = true")
        java.util.List<User> findAllClassCoordinators();

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
                        "WHERE (UPPER(r.name) LIKE '%TEACHER%' OR UPPER(r.name) LIKE '%FACULTY%') " +
                        "AND u.active = true AND (u.deleted = false OR u.deleted IS NULL) " +
                        "AND NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE UPPER(r2.name) LIKE '%ADMIN%' OR UPPER(r2.name) LIKE '%STUDENT%')")
        long countActiveGenuineTeachers();

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
                        "WHERE (UPPER(r.name) LIKE '%TEACHER%' OR UPPER(r.name) LIKE '%FACULTY%') " +
                        "AND (u.deleted = false OR u.deleted IS NULL)")
        long countAllTeachers();

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
                        "WHERE u.department.id = :departmentId " +
                        "AND (UPPER(r.name) LIKE '%TEACHER%' OR UPPER(r.name) LIKE '%FACULTY%') " +
                        "AND (u.deleted = false OR u.deleted IS NULL)")
        long countTeachersByDepartmentId(@org.springframework.data.repository.query.Param("departmentId") Long departmentId);
}
