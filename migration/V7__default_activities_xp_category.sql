-- Upgrade existing null values in xp_category to 'Academic' for backward compatibility
UPDATE `activities` SET `xp_category` = 'Academic' WHERE `xp_category` IS NULL;
