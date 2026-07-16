package com.spdms.repository;

import com.spdms.entity.XpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {
    List<XpTransaction> findByStudentStudentId(String studentId);
    List<XpTransaction> findByStudentIdAndStatus(Long studentId, String status);
    List<XpTransaction> findByStudentStudentIdAndCategory(String studentId, String category);
    Page<XpTransaction> findByStudentStudentId(String studentId, Pageable pageable);
    List<XpTransaction> findByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(tx.xpPoints), 0) FROM XpTransaction tx WHERE tx.student.id = :studentId AND tx.activity.id = :activityId AND tx.status = 'APPROVED'")
    int sumApprovedPointsByStudentAndActivity(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("activityId") Long activityId);
}
