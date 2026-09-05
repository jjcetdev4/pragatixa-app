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

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM section WHERE dept_id = :deptId AND section_name = :sectionName AND deleted = 1 LIMIT 1", nativeQuery = true)
    Optional<Section> findDeletedByDeptIdAndSectionName(@org.springframework.data.repository.query.Param("deptId") Long deptId, @org.springframework.data.repository.query.Param("sectionName") String sectionName);
}
