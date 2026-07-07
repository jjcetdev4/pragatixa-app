package com.spdms.repository;

import com.spdms.entity.Department;
import com.spdms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByDepartmentAndSectionName(Department department, String sectionName);
    long countByDepartmentId(Long departmentId);
}
