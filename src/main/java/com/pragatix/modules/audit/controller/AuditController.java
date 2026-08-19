package jjcet.PragatiX.modules.audit.controller;

import jjcet.PragatiX.dto.response.AuditLogDTO;
import jjcet.PragatiX.entity.AuditLog;
import jjcet.PragatiX.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        List<AuditLogDTO> dtos = logs.stream()
                .map(AuditLogDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
