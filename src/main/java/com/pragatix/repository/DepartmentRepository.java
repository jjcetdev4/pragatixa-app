package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    Optional<Department> findByDeptCode(String deptCode);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Department d WHERE d.deleted = false")
    java.util.List<Department> findAll();
}
