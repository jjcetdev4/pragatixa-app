package jjcet.PragatiX.modules.audit.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditLogSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AuditLogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            log.debug("Ensuring audit_logs.entity_id allows NULL for batch operations...");
            jdbcTemplate.execute("ALTER TABLE audit_logs MODIFY COLUMN entity_id BIGINT NULL");
            log.debug("Successfully ensured audit_logs.entity_id is NULLABLE.");
        } catch (Exception e) {
            log.debug("audit_logs schema check/alter: {}", e.getMessage());
        }
    }
}
