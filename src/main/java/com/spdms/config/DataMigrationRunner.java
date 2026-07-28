package com.spdms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DataMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting one-time data migration for Academic Year...");

        try {
            int stagesUpdated = jdbcTemplate.update("UPDATE activity_stages SET academic_year='SECOND_YEAR' WHERE academic_year IS NULL");
            log.info("Migrated {} stages to SECOND_YEAR.", stagesUpdated);
        } catch (Exception e) {
            log.error("Failed to migrate activity_stages: {}", e.getMessage());
        }

        try {
            int activitiesUpdated = jdbcTemplate.update("UPDATE activities SET assigned_academic_year='SECOND_YEAR' WHERE assigned_academic_year IS NULL");
            log.info("Migrated {} activities to SECOND_YEAR.", activitiesUpdated);
        } catch (Exception e) {
            log.error("Failed to migrate activities: {}", e.getMessage());
        }

        log.info("Data migration completed.");
    }
}
