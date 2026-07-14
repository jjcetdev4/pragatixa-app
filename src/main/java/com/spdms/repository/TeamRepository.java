package com.spdms.repository;

import com.spdms.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    boolean existsByName(String name);
    java.util.List<Team> findByAssignmentId(Long assignmentId);
    boolean existsByNameAndAssignmentId(String name, Long assignmentId);
}
