package com.pragatix.modules.faculty.repository;

import com.pragatix.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByUserUsername(String username);

    long countByDepartmentId(Long departmentId);
}
