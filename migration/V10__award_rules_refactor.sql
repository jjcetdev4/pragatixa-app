-- V10: Award Rules Refactor
-- Adds award_frequency and award_days columns to activities table
-- Migrates existing reset_period values and sets defaults

ALTER TABLE `activities`
    ADD COLUMN `award_frequency` VARCHAR(50)  NOT NULL DEFAULT 'One Time'  COMMENT 'One Time | Daily | Weekly | Monthly | Manual',
    ADD COLUMN `award_days`      VARCHAR(200) NULL     DEFAULT NULL         COMMENT 'Comma-separated working days for Weekly frequency e.g. Monday,Wednesday,Friday';

-- Migrate existing reset_period → award_frequency
UPDATE `activities` SET `award_frequency` = 'One Time'  WHERE LOWER(`reset_period`) IN ('once', 'one time', '');
UPDATE `activities` SET `award_frequency` = 'Daily'     WHERE LOWER(`reset_period`) = 'daily';
UPDATE `activities` SET `award_frequency` = 'Weekly'    WHERE LOWER(`reset_period`) = 'weekly';
UPDATE `activities` SET `award_frequency` = 'Monthly'   WHERE LOWER(`reset_period`) = 'monthly';
UPDATE `activities` SET `award_frequency` = 'Manual'    WHERE `award_frequency` = 'One Time' AND LOWER(`reset_period`) NOT IN ('once', 'one time', '', 'daily', 'weekly', 'monthly') AND `reset_period` IS NOT NULL AND `reset_period` != '';

-- Ensure one-time activities keep cap = 1
UPDATE `activities` SET `maximum_awards` = 1 WHERE `award_frequency` = 'One Time';
UPDATE `activities` SET `maximum_awards` = 1 WHERE `award_frequency` = 'Manual';

-- Index for fast lookups by frequency
CREATE INDEX idx_activities_award_frequency ON `activities`(`award_frequency`);
