-- V12: Make legacy subgroup column nullable if present on activities table
SET @dbname = DATABASE();
SET @tablename = 'activities';
SET @columnname = 'subgroup';
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
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
