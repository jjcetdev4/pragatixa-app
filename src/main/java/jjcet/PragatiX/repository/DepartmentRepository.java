package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByNameIgnoreCase(String name);
    Optional<Department> findByNameIgnoreCaseAndDeletedFalse(String name);

    Optional<Department> findByCode(String code);
    Optional<Department> findByCodeIgnoreCase(String code);
    Optional<Department> findByCodeIgnoreCaseAndDeletedFalse(String code);

    Optional<Department> findByDeptCode(String deptCode);
    Optional<Department> findByDeptCodeIgnoreCase(String deptCode);
    Optional<Department> findByDeptCodeIgnoreCaseAndDeletedFalse(String deptCode);

    List<Department> findByDepartmentTypeAndDeletedFalse(DepartmentType departmentType);
    List<Department> findByDepartmentType(DepartmentType departmentType);

    @Query("SELECT d FROM Department d WHERE d.deleted = false")
    List<Department> findAll();

    long countByDeletedFalse();
}
