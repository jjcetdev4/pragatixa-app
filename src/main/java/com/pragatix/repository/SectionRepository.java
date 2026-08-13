package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByDepartmentAndSectionName(Department department, String sectionName);

    long countByDepartment_Id(Long departmentId);

    java.util.List<Section> findByDepartment_Id(Long departmentId);

    java.util.List<Section> findByDepartment_IdOrderBySectionNameAsc(Long departmentId);

    void deleteByDepartment_Id(Long departmentId);
}
