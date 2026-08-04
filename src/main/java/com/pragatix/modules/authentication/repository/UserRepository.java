package com.pragatix.modules.authentication.repository;

import com.pragatix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByDepartmentId(Long departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    java.util.List<User> findByRoleName(@org.springframework.data.repository.query.Param("roleName") String roleName);

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
            "WHERE r.name = 'ROLE_TEACHER' " +
            "AND UPPER(sr.name) = 'CC' " +
            "AND u.active = true")
    java.util.List<User> findAllClassCoordinators();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
            "WHERE r.name = 'ROLE_TEACHER' AND u.active = true " +
            "AND NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.name IN ('ROLE_ADMIN', 'ROLE_STUDENT'))")
    long countActiveGenuineTeachers();
}
