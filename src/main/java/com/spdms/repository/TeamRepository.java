package com.spdms.repository;

import com.spdms.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    boolean existsByName(String name);
    java.util.List<Team> findByDepartmentIdAndYearAndSectionId(Long departmentId, String year, Long sectionId);
    boolean existsByNameAndDepartmentIdAndYearAndSectionId(String name, Long departmentId, String year, Long sectionId);
    
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.captain LEFT JOIN FETCH t.department LEFT JOIN FETCH t.section WHERE t.id = :id")
    Optional<Team> findByIdWithMembers(@org.springframework.data.repository.query.Param("id") Long id);
}
