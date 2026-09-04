package jjcet.PragatiX.modules.faculty.repository;

import jjcet.PragatiX.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Faculty f WHERE f.deleted = false")
    java.util.List<Faculty> findAll();

    Optional<Faculty> findByUserUsername(String username);

    Optional<Faculty> findByUserId(Long userId);

    long countByDepartmentId(Long departmentId);
}
