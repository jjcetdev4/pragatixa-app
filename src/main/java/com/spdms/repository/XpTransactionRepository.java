package com.spdms.repository;

import com.spdms.entity.XpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {
    List<XpTransaction> findByStudentRegNo(String regNo);
    List<XpTransaction> findByStudentIdAndStatus(Long regNo, String status);
    List<XpTransaction> findByStudentRegNoAndCategory(String regNo, String category);
    Page<XpTransaction> findByStudentRegNo(String regNo, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(tx.xpPoints), 0) FROM XpTransaction tx WHERE tx.student.id = :regNo AND tx.activity.id = :activityId AND tx.status = 'APPROVED'")
    int sumApprovedPointsByStudentAndActivity(@org.springframework.data.repository.query.Param("regNo") Long regNo, @org.springframework.data.repository.query.Param("activityId") Long activityId);
}
