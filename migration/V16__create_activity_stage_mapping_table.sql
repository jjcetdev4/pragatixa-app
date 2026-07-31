-- V16: Create activity_stage_mappings table for stage-activity mapping reuse
CREATE TABLE IF NOT EXISTS `activity_stage_mappings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `stage_id` BIGINT NOT NULL,
  `subgroup_id` BIGINT NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_stage_activity` (`stage_id`, `activity_id`),
  CONSTRAINT `fk_asm_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_asm_stage` FOREIGN KEY (`stage_id`) REFERENCES `stages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_asm_subgroup` FOREIGN KEY (`subgroup_id`) REFERENCES `activity_subgroups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Populate initial stage-activity mappings from existing activities table
INSERT IGNORE INTO activity_stage_mappings (activity_id, stage_id, subgroup_id, created_at)
SELECT a.id, sg.stage_id, a.subgroup_id, NOW()
FROM activities a
JOIN activity_subgroups sg ON a.subgroup_id = sg.id;
