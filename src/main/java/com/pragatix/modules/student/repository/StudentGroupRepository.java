package jjcet.PragatiX.modules.student.repository;

import jjcet.PragatiX.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    long countByDepartmentId(Long departmentId);
}
