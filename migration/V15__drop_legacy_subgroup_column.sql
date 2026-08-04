-- V15: Safely drop legacy VARCHAR subgroup column from activities table if present
SET @dbname = DATABASE();
SET @tablename = 'activities';
SET @columnname = 'subgroup';

-- 1. Modify to NULL first to prevent any lock or constraint failure
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'ALTER TABLE activities MODIFY COLUMN subgroup VARCHAR(255) NULL;',
  'SELECT 1;'
));
PREPARE stmt1 FROM @preparedStatement;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 2. Drop the obsolete subgroup column
SET @preparedStatement2 = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'ALTER TABLE activities DROP COLUMN subgroup;',
  'SELECT 1;'
));
PREPARE stmt2 FROM @preparedStatement2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
