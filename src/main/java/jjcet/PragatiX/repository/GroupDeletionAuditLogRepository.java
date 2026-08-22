package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.GroupDeletionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupDeletionAuditLogRepository extends JpaRepository<GroupDeletionAuditLog, Long> {
}
