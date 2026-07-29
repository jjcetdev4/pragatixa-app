package com.pragatix.repository;

import com.pragatix.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    long countByDepartmentId(Long departmentId);
}
