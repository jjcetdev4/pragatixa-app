package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.AuditLog;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    List<AuditLog> findByModuleOrderByCreatedAtDesc(AuditModule module);
    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);
    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);
}
