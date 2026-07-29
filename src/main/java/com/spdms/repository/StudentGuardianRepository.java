package com.pragatix.repository;

import com.pragatix.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {
    Optional<StudentGuardian> findByStudentId(Long studentId);

    Optional<StudentGuardian> findByRegNo(String regNo);

    List<StudentGuardian> findByStudentIdIn(List<Long> studentIds);
}
