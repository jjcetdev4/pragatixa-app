-- V13: Replace old uq_activity unique constraint on (category_id, activity_name) with uq_activity_subgroup on (subgroup_id, activity_name)
SET @dbname = DATABASE();
SET @tablename = 'activities';

-- 1. Drop old constraint uq_activity if it exists
SET @oldconstraint = 'uq_activity';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND CONSTRAINT_NAME = @oldconstraint
  ) > 0,
  'ALTER TABLE activities DROP INDEX uq_activity;',
  'SELECT 1;'
));
PREPARE dropIndex FROM @preparedStatement;
EXECUTE dropIndex;
DEALLOCATE PREPARE dropIndex;

-- 2. Add new unique constraint on (subgroup_id, activity_name) if not present
SET @newconstraint = 'uq_activity_subgroup';
SET @preparedStatement2 = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND CONSTRAINT_NAME = @newconstraint
  ) > 0,
  'SELECT 1;',
  'ALTER TABLE activities ADD CONSTRAINT uq_activity_subgroup UNIQUE (subgroup_id, activity_name);'
));
PREPARE addIndex FROM @preparedStatement2;
EXECUTE addIndex;
DEALLOCATE PREPARE addIndex;
