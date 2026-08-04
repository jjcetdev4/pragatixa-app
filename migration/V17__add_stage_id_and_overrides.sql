-- V17: Add stage_id to activity_assignments and stage override fields to activity_stage_mappings
SET @dbname = DATABASE();

-- 1. Add stage_id column to activity_assignments if not exists
SET @tablename = 'activity_assignments';
SET @columnname = 'stage_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname
  ) = 0,
  'ALTER TABLE activity_assignments ADD COLUMN stage_id BIGINT NULL;',
  'SELECT 1;'
));
PREPARE stmt1 FROM @preparedStatement;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 2. Add foreign key constraint for stage_id on activity_assignments if not exists
SET @constraintname = 'fk_aa_stage';
SET @preparedStatement2 = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND CONSTRAINT_NAME = @constraintname
  ) = 0,
  'ALTER TABLE activity_assignments ADD CONSTRAINT fk_aa_stage FOREIGN KEY (stage_id) REFERENCES stages(id) ON DELETE CASCADE;',
  'SELECT 1;'
));
PREPARE stmt2 FROM @preparedStatement2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3. Add override columns to activity_stage_mappings if not exist
ALTER TABLE activity_stage_mappings ADD COLUMN IF NOT EXISTS award_xp INT NULL;
ALTER TABLE activity_stage_mappings ADD COLUMN IF NOT EXISTS award_enabled BOOLEAN NULL;
ALTER TABLE activity_stage_mappings ADD COLUMN IF NOT EXISTS penalty_enabled BOOLEAN NULL;
ALTER TABLE activity_stage_mappings ADD COLUMN IF NOT EXISTS penalty_xp INT NULL;
ALTER TABLE activity_stage_mappings ADD COLUMN IF NOT EXISTS award_frequency VARCHAR(50) NULL;
