-- V21__fix_team_unique_constraint_to_class_level.sql
-- SPDMS - Scope Team Name Uniqueness to Class (name, department_id, year, section_id)

SET @dbname = DATABASE();
SET @tablename = 'teams';

-- 1. Drop any single-column unique index on 'name' if present (e.g. UKqcvyixaqvy6a1e3haycgy0nja, uk_team_name, uq_team_name, name)
-- Drop UKqcvyixaqvy6a1e3haycgy0nja specifically if it exists
SET @drop_uk_sql = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = @tablename
          AND INDEX_NAME = 'UKqcvyixaqvy6a1e3haycgy0nja'
    ) > 0,
    'ALTER TABLE teams DROP INDEX `UKqcvyixaqvy6a1e3haycgy0nja`;',
    'SELECT 1;'
));
PREPARE stmt_uk FROM @drop_uk_sql;
EXECUTE stmt_uk;
DEALLOCATE PREPARE stmt_uk;

DROP PROCEDURE IF EXISTS drop_single_column_team_name_unique_index;
DELIMITER $$
CREATE PROCEDURE drop_single_column_team_name_unique_index()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE idx_name VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT INDEX_NAME
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = @tablename
          AND COLUMN_NAME = 'name'
          AND NON_UNIQUE = 0
          AND INDEX_NAME != 'PRIMARY'
          AND INDEX_NAME != 'uk_team_name_class'
        GROUP BY INDEX_NAME
        HAVING COUNT(*) = 1;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO idx_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET @drop_sql = CONCAT('ALTER TABLE teams DROP INDEX `', idx_name, '`;');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL drop_single_column_team_name_unique_index();
DROP PROCEDURE IF EXISTS drop_single_column_team_name_unique_index;

-- 2. Ensure composite unique constraint uk_team_name_class exists
SET @comp_constraint = 'uk_team_name_class';
SET @comp_sql = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @dbname
          AND TABLE_NAME = @tablename
          AND INDEX_NAME = @comp_constraint
    ) > 0,
    'SELECT 1;',
    'ALTER TABLE teams ADD CONSTRAINT uk_team_name_class UNIQUE (name, department_id, year, section_id);'
));
PREPARE stmt_comp FROM @comp_sql;
EXECUTE stmt_comp;
DEALLOCATE PREPARE stmt_comp;
