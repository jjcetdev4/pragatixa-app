-- V22__fix_team_members_reg_no.sql
-- Drop legacy reg_no column from team_members to avoid Error 1364

DROP PROCEDURE IF EXISTS fix_team_members_columns;
DELIMITER $$
CREATE PROCEDURE fix_team_members_columns()
BEGIN
    DECLARE fk_name VARCHAR(100);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR 
        SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'team_members' AND COLUMN_NAME = 'reg_no' AND REFERENCED_TABLE_NAME IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'team_members'
          AND COLUMN_NAME = 'reg_no'
    ) THEN
        -- Drop foreign keys first
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO fk_name;
            IF done THEN
                LEAVE read_loop;
            END IF;
            SET @s = CONCAT('ALTER TABLE team_members DROP FOREIGN KEY `', fk_name, '`');
            PREPARE stmt FROM @s;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END LOOP;
        CLOSE cur;
        
        -- Drop column
        ALTER TABLE team_members DROP COLUMN reg_no;
    END IF;
END$$
DELIMITER ;

CALL fix_team_members_columns();
DROP PROCEDURE IF EXISTS fix_team_members_columns;
