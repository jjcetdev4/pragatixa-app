package jjcet.PragatiX.modules.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.entity.AuditLog;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.repository.AuditLogRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditModule module, String entityType, Long entityId, String description, Object oldValues, Object newValues) {
        try {
            Long actorUserId = null;
            String actorName = "SYSTEM";
            String actorRole = "SYSTEM";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                actorName = auth.getName();
                Optional<User> userOpt = userRepository.findByUsername(actorName);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    actorUserId = user.getId();
                    actorName = user.getFullName() != null && !user.getFullName().trim().isEmpty() ? user.getFullName() : user.getUsername();
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        actorRole = user.getRoles().iterator().next().getName();
                    } else {
                        actorRole = "USER";
                    }
                }
            }

            String oldValuesJson = null;
            String newValuesJson = null;

            try {
                if (oldValues != null) oldValuesJson = objectMapper.writeValueAsString(oldValues);
                if (newValues != null) newValuesJson = objectMapper.writeValueAsString(newValues);
            } catch (JsonProcessingException e) {
                oldValuesJson = "{\"error\": \"Could not serialize old values\"}";
                newValuesJson = "{\"error\": \"Could not serialize new values\"}";
            }

            AuditLog auditLog = new AuditLog(actorUserId, actorName, actorRole, action, module, entityType, entityId, description, oldValuesJson, newValuesJson);
            auditLogRepository.save(auditLog);
            log.info("Audit log recorded: [Action: {}, Module: {}, EntityType: {}, EntityId: {}, Actor: {}]",
                    action, module, entityType, entityId, actorName);
        } catch (Exception e) {
            log.warn("Failed to record audit log independently: {}", e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, AuditModule module, String entityType, Long entityId, String description) {
        log(action, module, entityType, entityId, description, null, null);
    }
}
