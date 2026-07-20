ALTER TABLE `activities`
ADD COLUMN `award_xp` INT DEFAULT 0,
ADD COLUMN `award_type` VARCHAR(50) DEFAULT 'Fixed XP',
ADD COLUMN `repeat_allowed` BOOLEAN DEFAULT FALSE,
ADD COLUMN `reset_period` VARCHAR(50) DEFAULT 'Once';

-- Migrate existing points and frequency configuration
UPDATE `activities` SET `award_xp` = CAST(COALESCE(`xp`, '0') AS SIGNED);
UPDATE `activities` SET `repeat_allowed` = TRUE WHERE `frequency` IS NOT NULL AND LOWER(`frequency`) != 'once' AND `frequency` != '';
UPDATE `activities` SET `reset_period` = COALESCE(`frequency`, 'Once');
