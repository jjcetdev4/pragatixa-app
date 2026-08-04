-- V14: Consolidate assigned_academic_year into academic_year on users table and drop any legacy assigned_academic_year columns
SET @dbname = DATABASE();
SET @tablename = 'users';

-- 1. If academic_year does not exist on users table, rename assigned_academic_year or add academic_year
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'academic_year') > 0,
  'SELECT 1;',
  IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'assigned_academic_year') > 0,
    'ALTER TABLE users CHANGE COLUMN assigned_academic_year academic_year VARCHAR(50) NULL;',
    'ALTER TABLE users ADD COLUMN academic_year VARCHAR(50) NULL;'
  )
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Drop assigned_academic_year column if both assigned_academic_year and academic_year exist on users table
SET @preparedStatement2 = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'assigned_academic_year') > 0
  AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'academic_year') > 0,
  'ALTER TABLE users DROP COLUMN assigned_academic_year;',
  'SELECT 1;'
));
PREPARE stmt2 FROM @preparedStatement2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3. Drop assigned_academic_year from activities table if present
SET @preparedStatement3 = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'activities' AND COLUMN_NAME = 'assigned_academic_year') > 0,
  'ALTER TABLE activities DROP COLUMN assigned_academic_year;',
  'SELECT 1;'
));
PREPARE stmt3 FROM @preparedStatement3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
