ALTER TABLE `activity_stages`
ADD COLUMN `start_date` DATE DEFAULT NULL,
ADD COLUMN `end_date` DATE DEFAULT NULL,
ADD COLUMN `display_order` INT NOT NULL DEFAULT 0,
ADD COLUMN `is_active` TINYINT(1) NOT NULL DEFAULT 1;

-- Initialize default values for existing rows (Stage 1)
UPDATE `activity_stages`
SET `start_date` = '2026-01-01',
    `end_date` = '2026-01-31',
    `display_order` = 1,
    `is_active` = 1
WHERE `id` = 1;
