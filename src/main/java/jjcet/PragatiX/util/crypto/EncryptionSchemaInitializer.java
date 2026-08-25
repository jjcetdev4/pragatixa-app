package jjcet.PragatiX.util.crypto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ensures database columns for encrypted fields (email, phone, mobile) have
 * sufficient length (VARCHAR(255)) and migrates any existing legacy plaintext values
 * to AES-256-GCM encrypted format.
 */
@Component
public class EncryptionSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(EncryptionSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public EncryptionSchemaInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.encryption.secret:PragatiXAES256GCMDatabaseEncryptionSecretKey2026!}") String encryptionSecret) {
        this.jdbcTemplate = jdbcTemplate;
        AesGcmEncryptionUtil.setSecretKey(encryptionSecret);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing AES-256-GCM database encryption schema & migration...");
        ensureColumnLengths();
        migrateExistingPlaintextData();
        log.info("AES-256-GCM database encryption initialized successfully.");
    }

    private void ensureColumnLengths() {
        String[] alterStatements = new String[] {
                "ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL",
                "ALTER TABLE users MODIFY COLUMN phone VARCHAR(255) NULL",
                "ALTER TABLE students MODIFY COLUMN email VARCHAR(255) NOT NULL",
                "ALTER TABLE students MODIFY COLUMN phone_no VARCHAR(255) NOT NULL",
                "ALTER TABLE students MODIFY COLUMN phone VARCHAR(255) NULL",
                "ALTER TABLE enrollments MODIFY COLUMN email VARCHAR(255) NOT NULL",
                "ALTER TABLE enrollments MODIFY COLUMN mobile VARCHAR(255) NOT NULL",
                "ALTER TABLE student_guardians MODIFY COLUMN email VARCHAR(255) NULL",
                "ALTER TABLE student_guardians MODIFY COLUMN phone_no VARCHAR(255) NOT NULL",
                "ALTER TABLE faculty MODIFY COLUMN phone_no VARCHAR(255) NOT NULL",
                "ALTER TABLE otp_tokens MODIFY COLUMN email VARCHAR(255) NOT NULL"
        };

        for (String sql : alterStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.debug("Column alter check/executed: {} (detail: {})", sql, e.getMessage());
            }
        }
    }

    private void migrateExistingPlaintextData() {
        try {
            // 1. Users
            migrateTableColumn("users", "id", "email");
            migrateTableColumn("users", "id", "phone");

            // 2. Students
            migrateTableColumn("students", "id", "email");
            migrateTableColumn("students", "id", "phone_no");
            migrateTableColumn("students", "id", "phone");

            // 3. Enrollments
            migrateTableColumn("enrollments", "id", "email");
            migrateTableColumn("enrollments", "id", "mobile");

            // 4. Student Guardians
            migrateTableColumn("student_guardians", "id", "email");
            migrateTableColumn("student_guardians", "id", "phone_no");

            // 5. Faculty
            migrateTableColumn("faculty", "id", "phone_no");

            // 6. Otp Tokens
            migrateTableColumn("otp_tokens", "id", "email");
        } catch (Exception e) {
            log.warn("Migration of existing unencrypted rows encountered an issue: {}", e.getMessage());
        }
    }

    private void migrateTableColumn(String tableName, String idCol, String colName) {
        try {
            String checkSql = "SELECT " + idCol + ", " + colName + " FROM " + tableName +
                    " WHERE " + colName + " IS NOT NULL AND " + colName + " != '' AND " + colName + " NOT LIKE 'ENC:%'";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(checkSql);
            if (rows.isEmpty()) {
                return;
            }

            log.info("Migrating {} unencrypted records in {}.{} to AES-256-GCM...", rows.size(), tableName, colName);
            for (Map<String, Object> row : rows) {
                Object id = row.get(idCol);
                Object val = row.get(colName);
                if (id != null && val != null) {
                    String plain = val.toString().trim();
                    if (!plain.isEmpty() && !plain.startsWith(AesGcmEncryptionUtil.PREFIX)) {
                        String encrypted = AesGcmEncryptionUtil.encrypt(plain);
                        jdbcTemplate.update(
                                "UPDATE " + tableName + " SET " + colName + " = ? WHERE " + idCol + " = ?",
                                encrypted, id
                        );
                    }
                }
            }
            log.info("Finished encrypting {}.{}", tableName, colName);
        } catch (Exception e) {
            log.debug("Table migration skip {}.{}: {}", tableName, colName, e.getMessage());
        }
    }
}
