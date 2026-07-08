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
    List<XpTransaction> findByStudentStudentIdAndCategory(String studentId, String category);
    Page<XpTransaction> findByStudentStudentId(String studentId, Pageable pageable);
    List<XpTransaction> findByStatus(String status);
}
