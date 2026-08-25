package jjcet.PragatiX.modules.enrollment.repository;

import jakarta.persistence.LockModeType;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.modules.enrollment.entity.Enrollment;
import jjcet.PragatiX.modules.enrollment.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByEmailAndIdNotAndDeletedFalse(String email, Long id);

    boolean existsByMobileAndDeletedFalse(String mobile);
    boolean existsByMobileAndIdNotAndDeletedFalse(String mobile, Long id);

    @Query("SELECT e.email FROM Enrollment e WHERE e.email IN :emails AND e.deleted = false")
    java.util.Set<String> findExistingEmailsIn(@Param("emails") java.util.Set<String> emails);

    @Query("SELECT e.mobile FROM Enrollment e WHERE e.mobile IN :mobiles AND e.deleted = false")
    java.util.Set<String> findExistingMobilesIn(@Param("mobiles") java.util.Set<String> mobiles);

    Optional<Enrollment> findByEmailAndDeletedFalse(String email);

    Optional<Enrollment> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.id = :id AND e.deleted = false")
    Optional<Enrollment> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT e.department FROM Enrollment e WHERE e.status = :status AND e.deleted = false ORDER BY e.department.name ASC")
    List<Department> findDistinctDepartmentsByStatus(@Param("status") EnrollmentStatus status);

    @Query("SELECT DISTINCT UPPER(SUBSTRING(e.fullName, 1, 1)) FROM Enrollment e WHERE e.department.id = :deptId AND e.status = :status AND e.deleted = false ORDER BY UPPER(SUBSTRING(e.fullName, 1, 1)) ASC")
    List<String> findDistinctAlphabetsByDeptAndStatus(@Param("deptId") Long deptId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.department.id = :deptId AND UPPER(e.fullName) LIKE CONCAT(UPPER(:letter), '%') AND e.status = :status AND e.deleted = false ORDER BY e.fullName ASC")
    List<Enrollment> findPendingStudentsByDeptAndLetter(@Param("deptId") Long deptId, @Param("letter") String letter, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.status = :status AND e.department.id = :deptId AND e.deleted = false AND LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Enrollment> findByStatusAndDepartmentIdAndSearch(
            @Param("status") EnrollmentStatus status,
            @Param("deptId") Long deptId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.status = :status AND e.department.id = :deptId AND e.deleted = false")
    Page<Enrollment> findByStatusAndDepartmentId(
            @Param("status") EnrollmentStatus status,
            @Param("deptId") Long deptId,
            Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.status = :status AND e.department.id IN :allowedDeptIds AND e.deleted = false AND LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Enrollment> findByStatusAndAllowedDeptIdsAndSearch(
            @Param("status") EnrollmentStatus status,
            @Param("allowedDeptIds") java.util.Set<Long> allowedDeptIds,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.status = :status AND e.department.id IN :allowedDeptIds AND e.deleted = false")
    Page<Enrollment> findByStatusAndAllowedDeptIds(
            @Param("status") EnrollmentStatus status,
            @Param("allowedDeptIds") java.util.Set<Long> allowedDeptIds,
            Pageable pageable);

    long countByStatusAndDeletedFalse(EnrollmentStatus status);
}
