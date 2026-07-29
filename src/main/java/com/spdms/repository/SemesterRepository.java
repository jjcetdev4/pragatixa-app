package com.pragatix.repository;

import com.pragatix.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findBySemesterNo(Byte semesterNo);

    Optional<Semester> findBySemesterName(String semesterName);
}
