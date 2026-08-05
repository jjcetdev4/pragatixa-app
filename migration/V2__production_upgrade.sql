-- ============================================================
-- V2__production_upgrade.sql
-- SPDMS - Enterprise Production Upgrade Script (MySQL 8.0+)
--
-- Upgrades the V1 schema from UUID-based BINARY(16) identifiers
-- to BIGINT AUTO_INCREMENT identifiers, registers missing tables
-- (students, subjects, activities, attendance, marks, guardians),
-- enforces database integrity, and optimizes query indexes.
-- ============================================================

-- Disable foreign key checks to prevent lock-outs during structural changes
SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ------------------------------------------------------------
-- 1. DROP CONSTRAINTS AND TRIGGERS
-- ------------------------------------------------------------

-- Drop V1 database triggers
DROP TRIGGER IF EXISTS trg_xp_after_insert;
DROP TRIGGER IF EXISTS trg_xp_after_update;
DROP TRIGGER IF EXISTS trg_xp_after_delete;

-- Drop all existing foreign key constraints on V1 tables
ALTER TABLE `departments` DROP FOREIGN KEY IF EXISTS `fk_departments_hod_user_id`;
ALTER TABLE `users` DROP FOREIGN KEY IF EXISTS `fk_users_department`;
ALTER TABLE `users` DROP FOREIGN KEY IF EXISTS `fk_users_status`;
ALTER TABLE `user_roles` DROP FOREIGN KEY IF EXISTS `fk_user_roles_user`;
ALTER TABLE `user_roles` DROP FOREIGN KEY IF EXISTS `fk_user_roles_role`;
ALTER TABLE `role_permissions` DROP FOREIGN KEY IF EXISTS `fk_role_permissions_role`;
ALTER TABLE `role_permissions` DROP FOREIGN KEY IF EXISTS `fk_role_permissions_permission`;
ALTER TABLE `student_profiles` DROP FOREIGN KEY IF EXISTS `fk_student_profiles_user`;
ALTER TABLE `faculty_profiles` DROP FOREIGN KEY IF EXISTS `fk_faculty_profiles_user`;
ALTER TABLE `student_parents` DROP FOREIGN KEY IF EXISTS `fk_student_parents_student`;
ALTER TABLE `student_parents` DROP FOREIGN KEY IF EXISTS `fk_student_parents_parent`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_stage`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_category`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_frequency`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_cap_period`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_evidence`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_owner`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_dependency`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_created`;
ALTER TABLE `rules` DROP FOREIGN KEY IF EXISTS `fk_rules_updated`;
ALTER TABLE `rule_conditions` DROP FOREIGN KEY IF EXISTS `fk_rule_conditions_rule`;
ALTER TABLE `student_groups` DROP FOREIGN KEY IF EXISTS `fk_groups_leader`;
ALTER TABLE `student_groups` DROP FOREIGN KEY IF EXISTS `fk_groups_created`;
ALTER TABLE `group_members` DROP FOREIGN KEY IF EXISTS `fk_gm_group`;
ALTER TABLE `group_members` DROP FOREIGN KEY IF EXISTS `fk_gm_student`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_student`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_rule`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_stage`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_group`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_status`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_evidence`;
ALTER TABLE `submissions` DROP FOREIGN KEY IF EXISTS `fk_sub_reviewer`;
ALTER TABLE `submission_evidence` DROP FOREIGN KEY IF EXISTS `fk_ev_submission`;
ALTER TABLE `submission_evidence` DROP FOREIGN KEY IF EXISTS `fk_ev_file_type`;
ALTER TABLE `violation_types` DROP FOREIGN KEY IF EXISTS `fk_vtype_severity`;
ALTER TABLE `violation_types` DROP FOREIGN KEY IF EXISTS `fk_vtype_created`;
ALTER TABLE `violation_types` DROP FOREIGN KEY IF EXISTS `fk_vtype_updated`;
ALTER TABLE `violations` DROP FOREIGN KEY IF EXISTS `fk_vio_student`;
ALTER TABLE `violations` DROP FOREIGN KEY IF EXISTS `fk_vio_type`;
ALTER TABLE `violations` DROP FOREIGN KEY IF EXISTS `fk_vio_reporter`;
ALTER TABLE `violations` DROP FOREIGN KEY IF EXISTS `fk_vio_status`;
ALTER TABLE `violations` DROP FOREIGN KEY IF EXISTS `fk_vio_approver`;
ALTER TABLE `violation_evidence` DROP FOREIGN KEY IF EXISTS `fk_ve_violation`;
ALTER TABLE `violation_evidence` DROP FOREIGN KEY IF EXISTS `fk_ve_file_type`;
ALTER TABLE `xp_transactions` DROP FOREIGN KEY IF EXISTS `fk_xp_student`;
ALTER TABLE `xp_transactions` DROP FOREIGN KEY IF EXISTS `fk_xp_source_type`;
ALTER TABLE `xp_transactions` DROP FOREIGN KEY IF EXISTS `fk_xp_submission`;
ALTER TABLE `xp_transactions` DROP FOREIGN KEY IF EXISTS `fk_xp_violation`;
ALTER TABLE `streaks` DROP FOREIGN KEY IF EXISTS `fk_streak_student`;
ALTER TABLE `streaks` DROP FOREIGN KEY IF EXISTS `fk_streak_rule`;
ALTER TABLE `notifications` DROP FOREIGN KEY IF EXISTS `fk_notif_user`;
ALTER TABLE `notifications` DROP FOREIGN KEY IF EXISTS `fk_notif_type`;
ALTER TABLE `notifications` DROP FOREIGN KEY IF EXISTS `fk_notif_submission`;
ALTER TABLE `notifications` DROP FOREIGN KEY IF EXISTS `fk_notif_violation`;
ALTER TABLE `student_stage_progress` DROP FOREIGN KEY IF EXISTS `fk_ssp_student`;
ALTER TABLE `student_stage_progress` DROP FOREIGN KEY IF EXISTS `fk_ssp_stage`;
ALTER TABLE `student_stage_progress` DROP FOREIGN KEY IF EXISTS `fk_ssp_prev_stage`;
ALTER TABLE `student_stage_progress` DROP FOREIGN KEY IF EXISTS `fk_ssp_promoted_by`;
ALTER TABLE `student_xp_summary` DROP FOREIGN KEY IF EXISTS `fk_sxs_student`;
ALTER TABLE `audit_logs` DROP FOREIGN KEY IF EXISTS `fk_audit_user`;
ALTER TABLE `login_history` DROP FOREIGN KEY IF EXISTS `fk_login_user`;

-- ------------------------------------------------------------
-- 2. RENAME TABLES FOR NAMING CONSISTENCY
-- ------------------------------------------------------------

-- Rename stages to activity_stages
RENAME TABLE `stages` TO `activity_stages`;

-- Rename student_groups to groups
RENAME TABLE `student_groups` TO `groups`;

-- ------------------------------------------------------------
-- 3. STRUCTURAL CONVERSION OF KEY COLUMNS (UUID to BIGINT)
-- ------------------------------------------------------------

-- Convert lookup tables to use BIGINT PKs
ALTER TABLE `user_statuses` DROP PRIMARY KEY;
ALTER TABLE `user_statuses` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `user_statuses` SET `old_id` = `id`;
ALTER TABLE `user_statuses` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `frequencies` DROP PRIMARY KEY;
ALTER TABLE `frequencies` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `frequencies` SET `old_id` = `id`;
ALTER TABLE `frequencies` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `cap_periods` DROP PRIMARY KEY;
ALTER TABLE `cap_periods` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `cap_periods` SET `old_id` = `id`;
ALTER TABLE `cap_periods` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `evidence_types` DROP PRIMARY KEY;
ALTER TABLE `evidence_types` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `evidence_types` SET `old_id` = `id`;
ALTER TABLE `evidence_types` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `notification_types` DROP PRIMARY KEY;
ALTER TABLE `notification_types` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `notification_types` SET `old_id` = `id`;
ALTER TABLE `notification_types` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `submission_statuses` DROP PRIMARY KEY;
ALTER TABLE `submission_statuses` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `submission_statuses` SET `old_id` = `id`;
ALTER TABLE `submission_statuses` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `violation_statuses` DROP PRIMARY KEY;
ALTER TABLE `violation_statuses` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `violation_statuses` SET `old_id` = `id`;
ALTER TABLE `violation_statuses` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `violation_severities` DROP PRIMARY KEY;
ALTER TABLE `violation_severities` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `violation_severities` SET `old_id` = `id`;
ALTER TABLE `violation_severities` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `xp_source_types` DROP PRIMARY KEY;
ALTER TABLE `xp_source_types` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `xp_source_types` SET `old_id` = `id`;
ALTER TABLE `xp_source_types` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `categories` DROP PRIMARY KEY;
ALTER TABLE `categories` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `categories` SET `old_id` = `id`;
ALTER TABLE `categories` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `permissions` DROP PRIMARY KEY;
ALTER TABLE `permissions` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `permissions` SET `old_id` = `id`;
ALTER TABLE `permissions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

-- Convert core organizational tables
ALTER TABLE `departments` DROP PRIMARY KEY;
ALTER TABLE `departments` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `departments` SET `old_id` = `id`;
ALTER TABLE `departments` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `departments` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `departments` MODIFY `code` VARCHAR(50) NOT NULL;

ALTER TABLE `roles` DROP PRIMARY KEY;
ALTER TABLE `roles` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `roles` SET `old_id` = `id`;
ALTER TABLE `roles` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `users` DROP PRIMARY KEY;
ALTER TABLE `users` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `users` SET `old_id` = `id`;
ALTER TABLE `users` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

-- Convert renamed relational tables
ALTER TABLE `activity_stages` DROP PRIMARY KEY;
ALTER TABLE `activity_stages` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `groups` DROP PRIMARY KEY;
ALTER TABLE `groups` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `groups` SET `old_id` = `id`;
ALTER TABLE `groups` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `groups` ADD COLUMN `size` INT NOT NULL DEFAULT 0;
ALTER TABLE `groups` ADD COLUMN `captain_id` BIGINT DEFAULT NULL;
ALTER TABLE `groups` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `groups` DROP COLUMN IF EXISTS `leader_id`;
ALTER TABLE `groups` DROP COLUMN IF EXISTS `is_deleted`;

-- ------------------------------------------------------------
-- 4. CREATE NEW PRODUCTION TABLES
-- ------------------------------------------------------------

-- Create subjects table
CREATE TABLE IF NOT EXISTS `subjects` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `idx_uq_subjects_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create students table
CREATE TABLE IF NOT EXISTS `students` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` VARCHAR(100) NOT NULL,
  `full_name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `gender` VARCHAR(20) DEFAULT NULL,
  `date_of_birth` DATE DEFAULT NULL,
  `address` TEXT DEFAULT NULL,
  `department_id` BIGINT DEFAULT NULL,
  `semester` VARCHAR(20) DEFAULT NULL,
  `academic_year` VARCHAR(50) DEFAULT NULL,
  `active` BOOLEAN NOT NULL DEFAULT TRUE,
  `score` INT NOT NULL DEFAULT 100,
  `group_id` BIGINT DEFAULT NULL,
  `spr_no` VARCHAR(100) DEFAULT NULL,
  `year` VARCHAR(10) DEFAULT NULL,
  `section` VARCHAR(50) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `idx_uq_students_student_id` UNIQUE (`student_id`),
  CONSTRAINT `idx_uq_students_email` UNIQUE (`email`),
  CONSTRAINT `chk_students_score` CHECK (`score` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create student_guardians table
CREATE TABLE IF NOT EXISTS `student_guardians` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `relationship` VARCHAR(50) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_student_guardians_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create activity_subgroups table
CREATE TABLE IF NOT EXISTS `activity_subgroups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `threshold` INT NOT NULL DEFAULT 0,
  `stage_id` BIGINT NOT NULL,
  `assigned_faculty_id` BIGINT DEFAULT NULL,
  `assigned_department_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create activities table
CREATE TABLE IF NOT EXISTS `activities` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `frequency` VARCHAR(100) DEFAULT NULL,
  `owner_department` VARCHAR(100) DEFAULT NULL,
  `owner_subrole` VARCHAR(100) DEFAULT NULL,
  `evidence` VARCHAR(255) DEFAULT NULL,
  `xp` VARCHAR(100) DEFAULT NULL,
  `xp_category` VARCHAR(100) DEFAULT NULL,
  `cap` VARCHAR(100) DEFAULT NULL,
  `type` VARCHAR(50) DEFAULT NULL,
  `justification` TEXT DEFAULT NULL,
  `category` VARCHAR(50) DEFAULT NULL,
  `subgroup_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create discipline_logs table
CREATE TABLE IF NOT EXISTS `discipline_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `points` INT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `student_id` BIGINT NOT NULL,
  `subgroup_id` BIGINT DEFAULT NULL,
  `recorded_by_id` BIGINT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create attendance table
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `date` DATE NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `recorded_by_id` BIGINT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_attendance_status` CHECK (`status` IN ('PRESENT', 'ABSENT', 'ONDUTY', 'LATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create marks table
CREATE TABLE IF NOT EXISTS `marks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `subject_id` BIGINT NOT NULL,
  `exam_type` VARCHAR(50) NOT NULL,
  `marks_obtained` DECIMAL(5,2) NOT NULL,
  `max_marks` DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  `recorded_by_id` BIGINT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_marks_obtained` CHECK (`marks_obtained` <= `max_marks`),
  CONSTRAINT `chk_marks_positive` CHECK (`marks_obtained` >= 0 AND `max_marks` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create system_settings table
CREATE TABLE IF NOT EXISTS `system_settings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `setting_key` VARCHAR(100) NOT NULL,
  `setting_value` TEXT DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `idx_uq_settings_key` UNIQUE (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 5. UPGRADE AND STANDARDIZE THE USERS TABLE
-- ------------------------------------------------------------

-- Add and modify columns in users to align with Spring Boot 3.x
ALTER TABLE `users` ADD COLUMN `username` VARCHAR(100) NULL;
ALTER TABLE `users` ADD COLUMN `full_name` VARCHAR(255) NULL;
ALTER TABLE `users` ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE `users` ADD COLUMN `temp_dept_id` BIGINT NULL;

-- Populate username from email prefix
UPDATE `users` SET `username` = SUBSTRING_INDEX(`email`, '@', 1);

-- Combine first_name and last_name into full_name
UPDATE `users` SET `full_name` = CONCAT(`first_name`, ' ', `last_name`);

-- Set active state based on deletion status
UPDATE `users` SET `active` = IF(`is_deleted` = 1, FALSE, TRUE);

-- Map old department binary id to new department bigint id
UPDATE `users` u JOIN `departments` d ON u.`department_id` = d.`old_id` SET u.`temp_dept_id` = d.`id`;

-- Finalize structural modifications of the users table
ALTER TABLE `users` MODIFY `username` VARCHAR(100) NOT NULL;
ALTER TABLE `users` MODIFY `full_name` VARCHAR(255) NOT NULL;
ALTER TABLE `users` MODIFY `email` VARCHAR(255) NOT NULL;
ALTER TABLE `users` CHANGE `password_hash` `password` VARCHAR(255) NOT NULL;
ALTER TABLE `users` DROP COLUMN `first_name`;
ALTER TABLE `users` DROP COLUMN `last_name`;
ALTER TABLE `users` DROP COLUMN `is_deleted`;
ALTER TABLE `users` DROP COLUMN `status_id`;
ALTER TABLE `users` DROP COLUMN `department_id`;
ALTER TABLE `users` CHANGE `temp_dept_id` `department_id` BIGINT NULL;

-- Alter section and year size constraints in users
ALTER TABLE `users` MODIFY `section` VARCHAR(50) DEFAULT NULL;
ALTER TABLE `users` MODIFY `year` VARCHAR(10) DEFAULT NULL;

-- ------------------------------------------------------------
-- 6. MIGRATE AND TRANSITION DATA TO NEW STRUCTURAL SCHEMA
-- ------------------------------------------------------------

-- Save user mapping helper column on students table temporarily
ALTER TABLE `students` ADD COLUMN `old_user_id` BINARY(16) NULL;

-- Migrate student profiles and details from users + student_profiles to students
INSERT INTO `students` (student_id, full_name, email, password, phone, active, score, year, section, old_user_id)
SELECT 
    sp.register_number,
    u.full_name,
    u.email,
    u.password,
    u.phone,
    u.active,
    100, -- Initialize default starting score to 100
    CAST(sp.current_year AS CHAR),
    sp.section,
    u.old_id
FROM `users` u
JOIN `student_profiles` sp ON u.`old_id` = sp.`user_id`;

-- Map student departments
UPDATE `students` s
JOIN `users` u ON s.old_user_id = u.old_id
SET s.department_id = u.department_id;

-- Seed user_sub_roles table
CREATE TABLE IF NOT EXISTS `user_sub_roles` (
  `user_id` BIGINT NOT NULL,
  `sub_role` VARCHAR(150) NOT NULL,
  PRIMARY KEY (`user_id`, `sub_role`),
  CONSTRAINT `fk_usr_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate faculty designations to sub_roles
INSERT INTO `user_sub_roles` (user_id, sub_role)
SELECT DISTINCT u.id, fp.designation
FROM `users` u
JOIN `faculty_profiles` fp ON u.`old_id` = fp.`user_id`
WHERE fp.designation IS NOT NULL;

-- Seed coordinators role
INSERT INTO `user_sub_roles` (user_id, sub_role)
SELECT DISTINCT u.id, 'CC'
FROM `users` u
JOIN `faculty_profiles` fp ON u.`old_id` = fp.`user_id`
WHERE fp.is_class_coordinator = TRUE;

-- ------------------------------------------------------------
-- 7. CONVERT REMAINING SCHEMA MAPPING TABLES (UUID to BIGINT)
-- ------------------------------------------------------------

-- user_roles
ALTER TABLE `user_roles` ADD COLUMN `temp_user_id` BIGINT NULL;
ALTER TABLE `user_roles` ADD COLUMN `temp_role_id` BIGINT NULL;
UPDATE `user_roles` ur JOIN `users` u ON ur.`user_id` = u.`old_id` SET ur.`temp_user_id` = u.`id`;
UPDATE `user_roles` ur JOIN `roles` r ON ur.`role_id` = r.`old_id` SET ur.`temp_role_id` = r.`id`;
ALTER TABLE `user_roles` DROP PRIMARY KEY;
ALTER TABLE `user_roles` DROP COLUMN `user_id`;
ALTER TABLE `user_roles` DROP COLUMN `role_id`;
ALTER TABLE `user_roles` CHANGE `temp_user_id` `user_id` BIGINT NOT NULL;
ALTER TABLE `user_roles` CHANGE `temp_role_id` `role_id` BIGINT NOT NULL;
ALTER TABLE `user_roles` ADD PRIMARY KEY (`user_id`, `role_id`);

-- role_permissions
ALTER TABLE `role_permissions` ADD COLUMN `temp_role_id` BIGINT NULL;
ALTER TABLE `role_permissions` ADD COLUMN `temp_permission_id` BIGINT NULL;
UPDATE `role_permissions` rp JOIN `roles` r ON rp.`role_id` = r.`old_id` SET rp.`temp_role_id` = r.`id`;
UPDATE `role_permissions` rp JOIN `permissions` p ON rp.`permission_id` = p.`old_id` SET rp.`temp_permission_id` = p.`id`;
ALTER TABLE `role_permissions` DROP PRIMARY KEY;
ALTER TABLE `role_permissions` DROP COLUMN `role_id`;
ALTER TABLE `role_permissions` DROP COLUMN `permission_id`;
ALTER TABLE `role_permissions` CHANGE `temp_role_id` `role_id` BIGINT NOT NULL;
ALTER TABLE `role_permissions` CHANGE `temp_permission_id` `permission_id` BIGINT NOT NULL;
ALTER TABLE `role_permissions` ADD PRIMARY KEY (`role_id`, `permission_id`);

-- group_members
ALTER TABLE `group_members` ADD COLUMN `temp_group_id` BIGINT NULL;
ALTER TABLE `group_members` ADD COLUMN `temp_student_id` BIGINT NULL;
UPDATE `group_members` gm JOIN `groups` g ON gm.`group_id` = g.`old_id` SET gm.`temp_group_id` = g.`id`;
UPDATE `group_members` gm JOIN `students` s ON gm.`student_id` = s.`old_user_id` SET gm.`temp_student_id` = s.`id`;
ALTER TABLE `group_members` DROP PRIMARY KEY;
ALTER TABLE `group_members` DROP COLUMN `group_id`;
ALTER TABLE `group_members` DROP COLUMN `student_id`;
ALTER TABLE `group_members` CHANGE `temp_group_id` `group_id` BIGINT NOT NULL;
ALTER TABLE `group_members` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `group_members` ADD PRIMARY KEY (`group_id`, `student_id`);

-- Migrate group members to students.group_id
UPDATE `students` s
JOIN `group_members` gm ON s.`id` = gm.`student_id`
SET s.`group_id` = gm.`group_id`;

-- Map captain_id on groups
UPDATE `groups` g
JOIN `users` u ON g.`created_by` = u.`old_id`
SET g.`captain_id` = (SELECT `id` FROM `students` WHERE `old_user_id` = u.`old_id` LIMIT 1);

-- rules
ALTER TABLE `rules` ADD COLUMN `temp_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_stage_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_category_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_frequency_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_cap_period_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_evidence_type_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_owner_role_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_dependency_rule_id` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_created_by` BIGINT NULL;
ALTER TABLE `rules` ADD COLUMN `temp_updated_by` BIGINT NULL;

ALTER TABLE `rules` DROP PRIMARY KEY;
ALTER TABLE `rules` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

UPDATE `rules` r JOIN `activity_stages` s ON r.`stage_id` = s.`id` SET r.`temp_stage_id` = s.`id`;
UPDATE `rules` r JOIN `categories` c ON r.`category_id` = c.`old_id` SET r.`temp_category_id` = c.`id`;
UPDATE `rules` r JOIN `frequencies` f ON r.`frequency_id` = f.`old_id` SET r.`temp_frequency_id` = f.`id`;
UPDATE `rules` r JOIN `cap_periods` cp ON r.`cap_period_id` = cp.`old_id` SET r.`temp_cap_period_id` = cp.`id`;
UPDATE `rules` r JOIN `evidence_types` e ON r.`evidence_type_id` = e.`old_id` SET r.`temp_evidence_type_id` = e.`id`;
UPDATE `rules` r JOIN `roles` ro ON r.`owner_role_id` = ro.`old_id` SET r.`temp_owner_role_id` = ro.`id`;
UPDATE `rules` r JOIN `users` u ON r.`created_by` = u.`old_id` SET r.`temp_created_by` = u.`id`;
UPDATE `rules` r JOIN `users` u ON r.`updated_by` = u.`old_id` SET r.`temp_updated_by` = u.`id`;

ALTER TABLE `rules` DROP COLUMN `stage_id`;
ALTER TABLE `rules` DROP COLUMN `category_id`;
ALTER TABLE `rules` DROP COLUMN `frequency_id`;
ALTER TABLE `rules` DROP COLUMN `cap_period_id`;
ALTER TABLE `rules` DROP COLUMN `evidence_type_id`;
ALTER TABLE `rules` DROP COLUMN `owner_role_id`;
ALTER TABLE `rules` DROP COLUMN `dependency_rule_id`;
ALTER TABLE `rules` DROP COLUMN `created_by`;
ALTER TABLE `rules` DROP COLUMN `updated_by`;

ALTER TABLE `rules` CHANGE `temp_stage_id` `stage_id` BIGINT NOT NULL;
ALTER TABLE `rules` CHANGE `temp_category_id` `category_id` BIGINT NOT NULL;
ALTER TABLE `rules` CHANGE `temp_frequency_id` `frequency_id` BIGINT NOT NULL;
ALTER TABLE `rules` CHANGE `temp_cap_period_id` `cap_period_id` BIGINT NULL;
ALTER TABLE `rules` CHANGE `temp_evidence_type_id` `evidence_type_id` BIGINT NOT NULL;
ALTER TABLE `rules` CHANGE `temp_owner_role_id` `owner_role_id` BIGINT NOT NULL;
ALTER TABLE `rules` CHANGE `temp_dependency_rule_id` `dependency_rule_id` BIGINT NULL;
ALTER TABLE `rules` CHANGE `temp_created_by` `created_by` BIGINT NULL;
ALTER TABLE `rules` CHANGE `temp_updated_by` `updated_by` BIGINT NULL;

-- rule_conditions
ALTER TABLE `rule_conditions` ADD COLUMN `temp_rule_id` BIGINT NULL;
UPDATE `rule_conditions` rc JOIN `rules` r ON rc.`rule_id` = r.`id` SET rc.`temp_rule_id` = r.`id`;
ALTER TABLE `rule_conditions` DROP PRIMARY KEY;
ALTER TABLE `rule_conditions` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `rule_conditions` SET `old_id` = `id`;
ALTER TABLE `rule_conditions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `rule_conditions` DROP COLUMN `rule_id`;
ALTER TABLE `rule_conditions` CHANGE `temp_rule_id` `rule_id` BIGINT NOT NULL;

-- submissions
ALTER TABLE `submissions` ADD COLUMN `temp_student_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_rule_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_stage_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_group_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_status_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_evidence_type_id` BIGINT NULL;
ALTER TABLE `submissions` ADD COLUMN `temp_reviewed_by` BIGINT NULL;

UPDATE `submissions` s JOIN `students` st ON s.`student_id` = st.`old_user_id` SET s.`temp_student_id` = st.`id`;
UPDATE `submissions` s JOIN `rules` r ON s.`rule_id` = r.`id` SET s.`temp_rule_id` = r.`id`;
UPDATE `submissions` s JOIN `activity_stages` ast ON s.`stage_id` = ast.`id` SET s.`temp_stage_id` = ast.`id`;
UPDATE `submissions` s JOIN `groups` g ON s.`group_id` = g.`old_id` SET s.`temp_group_id` = g.`id`;
UPDATE `submissions` s JOIN `submission_statuses` ss ON s.`status_id` = ss.`old_id` SET s.`temp_status_id` = ss.`id`;
UPDATE `submissions` s JOIN `evidence_types` et ON s.`evidence_type_id` = et.`old_id` SET s.`temp_evidence_type_id` = et.`id`;
UPDATE `submissions` s JOIN `users` u ON s.`reviewed_by` = u.`old_id` SET s.`temp_reviewed_by` = u.`id`;

ALTER TABLE `submissions` DROP PRIMARY KEY;
ALTER TABLE `submissions` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `submissions` SET `old_id` = `id`;
ALTER TABLE `submissions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `submissions` DROP COLUMN `student_id`;
ALTER TABLE `submissions` DROP COLUMN `rule_id`;
ALTER TABLE `submissions` DROP COLUMN `stage_id`;
ALTER TABLE `submissions` DROP COLUMN `group_id`;
ALTER TABLE `submissions` DROP COLUMN `status_id`;
ALTER TABLE `submissions` DROP COLUMN `evidence_type_id`;
ALTER TABLE `submissions` DROP COLUMN `reviewed_by`;

ALTER TABLE `submissions` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `submissions` CHANGE `temp_rule_id` `rule_id` BIGINT NOT NULL;
ALTER TABLE `submissions` CHANGE `temp_stage_id` `stage_id` BIGINT NOT NULL;
ALTER TABLE `submissions` CHANGE `temp_group_id` `group_id` BIGINT NULL;
ALTER TABLE `submissions` CHANGE `temp_status_id` `status_id` BIGINT NOT NULL;
ALTER TABLE `submissions` CHANGE `temp_evidence_type_id` `evidence_type_id` BIGINT NOT NULL;
ALTER TABLE `submissions` CHANGE `temp_reviewed_by` `reviewed_by` BIGINT NULL;

-- submission_evidence
ALTER TABLE `submission_evidence` ADD COLUMN `temp_submission_id` BIGINT NULL;
ALTER TABLE `submission_evidence` ADD COLUMN `temp_file_type_id` BIGINT NULL;
UPDATE `submission_evidence` se JOIN `submissions` s ON se.`submission_id` = s.`old_id` SET se.`temp_submission_id` = s.`id`;
UPDATE `submission_evidence` se JOIN `evidence_types` et ON se.`file_type_id` = et.`old_id` SET se.`temp_file_type_id` = et.`id`;
ALTER TABLE `submission_evidence` DROP PRIMARY KEY;
ALTER TABLE `submission_evidence` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `submission_evidence` SET `old_id` = `id`;
ALTER TABLE `submission_evidence` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `submission_evidence` DROP COLUMN `submission_id`;
ALTER TABLE `submission_evidence` DROP COLUMN `file_type_id`;
ALTER TABLE `submission_evidence` CHANGE `temp_submission_id` `submission_id` BIGINT NOT NULL;
ALTER TABLE `submission_evidence` CHANGE `temp_file_type_id` `file_type_id` BIGINT NOT NULL;

-- violation_types
ALTER TABLE `violation_types` ADD COLUMN `temp_severity_id` BIGINT NULL;
ALTER TABLE `violation_types` ADD COLUMN `temp_created_by` BIGINT NULL;
ALTER TABLE `violation_types` ADD COLUMN `temp_updated_by` BIGINT NULL;
UPDATE `violation_types` vt JOIN `violation_severities` vs ON vt.`severity_id` = vs.`old_id` SET vt.`temp_severity_id` = vs.`id`;
UPDATE `violation_types` vt JOIN `users` u ON vt.`created_by` = u.`old_id` SET vt.`temp_created_by` = u.`id`;
UPDATE `violation_types` vt JOIN `users` u ON vt.`updated_by` = u.`old_id` SET vt.`temp_updated_by` = u.`id`;
ALTER TABLE `violation_types` DROP PRIMARY KEY;
ALTER TABLE `violation_types` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `violation_types` SET `old_id` = `id`;
ALTER TABLE `violation_types` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `violation_types` DROP COLUMN `severity_id`;
ALTER TABLE `violation_types` DROP COLUMN `created_by`;
ALTER TABLE `violation_types` DROP COLUMN `updated_by`;
ALTER TABLE `violation_types` CHANGE `temp_severity_id` `severity_id` BIGINT NOT NULL;
ALTER TABLE `violation_types` CHANGE `temp_created_by` `created_by` BIGINT NULL;
ALTER TABLE `violation_types` CHANGE `temp_updated_by` `updated_by` BIGINT NULL;

-- violations
ALTER TABLE `violations` ADD COLUMN `temp_student_id` BIGINT NULL;
ALTER TABLE `violations` ADD COLUMN `temp_violation_type_id` BIGINT NULL;
ALTER TABLE `violations` ADD COLUMN `temp_reported_by` BIGINT NULL;
ALTER TABLE `violations` ADD COLUMN `temp_status_id` BIGINT NULL;
ALTER TABLE `violations` ADD COLUMN `temp_approved_by` BIGINT NULL;

UPDATE `violations` v JOIN `students` s ON v.`student_id` = s.`old_user_id` SET v.`temp_student_id` = s.`id`;
UPDATE `violations` v JOIN `violation_types` vt ON v.`violation_type_id` = vt.`old_id` SET v.`temp_violation_type_id` = vt.`id`;
UPDATE `violations` v JOIN `users` u ON v.`reported_by` = u.`old_id` SET v.`temp_reported_by` = u.`id`;
UPDATE `violations` v JOIN `violation_statuses` vs ON v.`status_id` = vs.`old_id` SET v.`temp_status_id` = vs.`id`;
UPDATE `violations` v JOIN `users` u ON v.`approved_by` = u.`old_id` SET v.`temp_approved_by` = u.`id`;

ALTER TABLE `violations` DROP PRIMARY KEY;
ALTER TABLE `violations` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `violations` SET `old_id` = `id`;
ALTER TABLE `violations` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `violations` DROP COLUMN `student_id`;
ALTER TABLE `violations` DROP COLUMN `violation_type_id`;
ALTER TABLE `violations` DROP COLUMN `reported_by`;
ALTER TABLE `violations` DROP COLUMN `status_id`;
ALTER TABLE `violations` DROP COLUMN `approved_by`;

ALTER TABLE `violations` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `violations` CHANGE `temp_violation_type_id` `violation_type_id` BIGINT NOT NULL;
ALTER TABLE `violations` CHANGE `temp_reported_by` `reported_by` BIGINT NOT NULL;
ALTER TABLE `violations` CHANGE `temp_status_id` `status_id` BIGINT NOT NULL;
ALTER TABLE `violations` CHANGE `temp_approved_by` `approved_by` BIGINT NULL;

-- violation_evidence
ALTER TABLE `violation_evidence` ADD COLUMN `temp_violation_id` BIGINT NULL;
ALTER TABLE `violation_evidence` ADD COLUMN `temp_file_type_id` BIGINT NULL;
UPDATE `violation_evidence` ve JOIN `violations` v ON ve.`violation_id` = v.`old_id` SET ve.`temp_violation_id` = v.`id`;
UPDATE `violation_evidence` ve JOIN `evidence_types` et ON ve.`file_type_id` = et.`old_id` SET ve.`temp_file_type_id` = et.`id`;
ALTER TABLE `violation_evidence` DROP PRIMARY KEY;
ALTER TABLE `violation_evidence` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `violation_evidence` SET `old_id` = `id`;
ALTER TABLE `violation_evidence` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `violation_evidence` DROP COLUMN `violation_id`;
ALTER TABLE `violation_evidence` DROP COLUMN `file_type_id`;
ALTER TABLE `violation_evidence` CHANGE `temp_violation_id` `violation_id` BIGINT NOT NULL;
ALTER TABLE `violation_evidence` CHANGE `temp_file_type_id` `file_type_id` BIGINT NOT NULL;

-- xp_transactions
ALTER TABLE `xp_transactions` ADD COLUMN `temp_student_id` BIGINT NULL;
ALTER TABLE `xp_transactions` ADD COLUMN `temp_source_type_id` BIGINT NULL;
ALTER TABLE `xp_transactions` ADD COLUMN `temp_submission_id` BIGINT NULL;
ALTER TABLE `xp_transactions` ADD COLUMN `temp_violation_id` BIGINT NULL;

UPDATE `xp_transactions` xt JOIN `students` s ON xt.`student_id` = s.`old_user_id` SET xt.`temp_student_id` = s.`id`;
UPDATE `xp_transactions` xt JOIN `xp_source_types` xst ON xt.`source_type_id` = xst.`old_id` SET xt.`temp_source_type_id` = xst.`id`;
UPDATE `xp_transactions` xt JOIN `submissions` s ON xt.`submission_id` = s.`old_id` SET xt.`temp_submission_id` = s.`id`;
UPDATE `xp_transactions` xt JOIN `violations` v ON xt.`violation_id` = v.`old_id` SET xt.`temp_violation_id` = v.`id`;

ALTER TABLE `xp_transactions` DROP PRIMARY KEY;
ALTER TABLE `xp_transactions` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `xp_transactions` SET `old_id` = `id`;
ALTER TABLE `xp_transactions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);

ALTER TABLE `xp_transactions` DROP COLUMN `student_id`;
ALTER TABLE `xp_transactions` DROP COLUMN `source_type_id`;
ALTER TABLE `xp_transactions` DROP COLUMN `submission_id`;
ALTER TABLE `xp_transactions` DROP COLUMN `violation_id`;

ALTER TABLE `xp_transactions` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `xp_transactions` CHANGE `temp_source_type_id` `source_type_id` BIGINT NOT NULL;
ALTER TABLE `xp_transactions` CHANGE `temp_submission_id` `submission_id` BIGINT NULL;
ALTER TABLE `xp_transactions` CHANGE `temp_violation_id` `violation_id` BIGINT NULL;

-- streaks
ALTER TABLE `streaks` ADD COLUMN `temp_student_id` BIGINT NULL;
ALTER TABLE `streaks` ADD COLUMN `temp_rule_id` BIGINT NULL;
UPDATE `streaks` s JOIN `students` st ON s.`student_id` = st.`old_user_id` SET s.`temp_student_id` = st.`id`;
UPDATE `streaks` s JOIN `rules` r ON s.`rule_id` = r.`id` SET s.`temp_rule_id` = r.`id`;
ALTER TABLE `streaks` DROP PRIMARY KEY;
ALTER TABLE `streaks` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `streaks` SET `old_id` = `id`;
ALTER TABLE `streaks` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `streaks` DROP COLUMN `student_id`;
ALTER TABLE `streaks` DROP COLUMN `rule_id`;
ALTER TABLE `streaks` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `streaks` CHANGE `temp_rule_id` `rule_id` BIGINT NOT NULL;

-- notifications
ALTER TABLE `notifications` ADD COLUMN `recipient_id` BIGINT NULL;
ALTER TABLE `notifications` ADD COLUMN `recipient_type` VARCHAR(50) NOT NULL DEFAULT 'USER';
ALTER TABLE `notifications` ADD COLUMN `message` TEXT NULL;
UPDATE `notifications` n JOIN `users` u ON n.`user_id` = u.`old_id` SET n.`recipient_id` = u.`id`;
UPDATE `notifications` SET `message` = `body`;
ALTER TABLE `notifications` DROP PRIMARY KEY;
ALTER TABLE `notifications` ADD COLUMN `old_id` BINARY(16) NULL;
UPDATE `notifications` SET `old_id` = `id`;
ALTER TABLE `notifications` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`);
ALTER TABLE `notifications` CHANGE `is_read` `read_status` BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE `notifications` DROP COLUMN `user_id`;
ALTER TABLE `notifications` DROP COLUMN `type_id`;
ALTER TABLE `notifications` DROP COLUMN `body`;
ALTER TABLE `notifications` DROP COLUMN `related_submission_id`;
ALTER TABLE `notifications` DROP COLUMN `related_violation_id`;
ALTER TABLE `notifications` DROP COLUMN `read_at`;
ALTER TABLE `notifications` MODIFY `recipient_type` VARCHAR(50) NOT NULL;
ALTER TABLE `notifications` MODIFY `message` TEXT NOT NULL;
ALTER TABLE `notifications` MODIFY `title` VARCHAR(255) NOT NULL;

-- student_stage_progress
ALTER TABLE `student_stage_progress` ADD COLUMN `temp_student_id` BIGINT NULL;
ALTER TABLE `student_stage_progress` ADD COLUMN `temp_current_stage_id` BIGINT NULL;
ALTER TABLE `student_stage_progress` ADD COLUMN `temp_previous_stage_id` BIGINT NULL;
ALTER TABLE `student_stage_progress` ADD COLUMN `temp_promoted_by` BIGINT NULL;

UPDATE `student_stage_progress` ssp JOIN `students` s ON ssp.`student_id` = s.`old_user_id` SET ssp.`temp_student_id` = s.`id`;
UPDATE `student_stage_progress` ssp JOIN `activity_stages` ast ON ssp.`current_stage_id` = ast.`id` SET ssp.`temp_current_stage_id` = ast.`id`;
UPDATE `student_stage_progress` ssp JOIN `activity_stages` ast ON ssp.`previous_stage_id` = ast.`id` SET ssp.`temp_previous_stage_id` = ast.`id`;
UPDATE `student_stage_progress` ssp JOIN `users` u ON ssp.`promoted_by` = u.`old_id` SET ssp.`temp_promoted_by` = u.`id`;

ALTER TABLE `student_stage_progress` DROP PRIMARY KEY;
ALTER TABLE `student_stage_progress` DROP COLUMN `student_id`;
ALTER TABLE `student_stage_progress` DROP COLUMN `current_stage_id`;
ALTER TABLE `student_stage_progress` DROP COLUMN `previous_stage_id`;
ALTER TABLE `student_stage_progress` DROP COLUMN `promoted_by`;

ALTER TABLE `student_stage_progress` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `student_stage_progress` CHANGE `temp_current_stage_id` `current_stage_id` BIGINT NOT NULL;
ALTER TABLE `student_stage_progress` CHANGE `temp_previous_stage_id` `previous_stage_id` BIGINT NULL;
ALTER TABLE `student_stage_progress` CHANGE `temp_promoted_by` `promoted_by` BIGINT NULL;
ALTER TABLE `student_stage_progress` ADD PRIMARY KEY (`student_id`);

-- student_xp_summary
ALTER TABLE `student_xp_summary` ADD COLUMN `temp_student_id` BIGINT NULL;
UPDATE `student_xp_summary` sxs JOIN `students` s ON sxs.`student_id` = s.`old_user_id` SET sxs.`temp_student_id` = s.`id`;
ALTER TABLE `student_xp_summary` DROP PRIMARY KEY;
ALTER TABLE `student_xp_summary` DROP COLUMN `student_id`;
ALTER TABLE `student_xp_summary` CHANGE `temp_student_id` `student_id` BIGINT NOT NULL;
ALTER TABLE `student_xp_summary` ADD PRIMARY KEY (`student_id`);

-- audit_logs
ALTER TABLE `audit_logs` ADD COLUMN `temp_user_id` BIGINT NULL;
UPDATE `audit_logs` al JOIN `users` u ON al.`performed_by` = u.`old_id` SET al.`temp_user_id` = u.`id`;
ALTER TABLE `audit_logs` DROP COLUMN `performed_by`;
ALTER TABLE `audit_logs` CHANGE `temp_user_id` `user_id` BIGINT NULL;
ALTER TABLE `audit_logs` MODIFY `entity_id` BIGINT NULL;
ALTER TABLE `audit_logs` MODIFY `action` VARCHAR(100) NOT NULL;
ALTER TABLE `audit_logs` MODIFY `entity_name` VARCHAR(100) NOT NULL;
ALTER TABLE `audit_logs` MODIFY `old_value` TEXT DEFAULT NULL;
ALTER TABLE `audit_logs` MODIFY `new_value` TEXT DEFAULT NULL;

-- login_history
ALTER TABLE `login_history` ADD COLUMN `temp_user_id` BIGINT NULL;
UPDATE `login_history` lh JOIN `users` u ON lh.`user_id` = u.`old_id` SET lh.`temp_user_id` = u.`id`;
ALTER TABLE `login_history` DROP COLUMN `user_id`;
ALTER TABLE `login_history` CHANGE `temp_user_id` `user_id` BIGINT NOT NULL;

-- ------------------------------------------------------------
-- 8. MIGRATE XP TRANSACTIONS HISTORY TO DISCIPLINE LOGS
-- ------------------------------------------------------------

-- Migrate XP adjustments from transactional logs into discipline_logs
INSERT INTO `discipline_logs` (points, reason, student_id, subgroup_id, recorded_by_id, created_at)
SELECT 
    tx.xp_change,
    COALESCE(tx.reason, 'Migrated XP Transaction'),
    s.id,
    NULL, -- Set default null for subgroup
    1, -- Fallback to system admin user (id = 1)
    tx.created_at
FROM `xp_transactions` tx
JOIN `students` s ON tx.student_id = s.id;

-- ------------------------------------------------------------
-- 9. LOOKUP TABLE DATA SEEDING AND IMPROVEMENTS
-- ------------------------------------------------------------

-- Seed role definitions starting with static integer IDs
INSERT INTO `roles` (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_TEACHER'),
(3, 'ROLE_STUDENT'),
(4, 'ROLE_TRANSPORT')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Seed default Administrator credentials (Password '12345' BCrypt hash)
INSERT INTO `users` (id, username, password, full_name, email, active, department_id) VALUES
(1, 'admin', '$2a$10$tZ261jH4J4T7Yw8xU43uReL1Y/D19Q59Z0bUoH7hL8.z5R0Wd231e', 'System Administrator', 'admin@pragatix.com', TRUE, NULL)
ON DUPLICATE KEY UPDATE username = VALUES(username), password = VALUES(password), full_name = VALUES(full_name), email = VALUES(email), department_id = VALUES(department_id);

-- Assign ROLE_ADMIN to default admin user
INSERT INTO `user_roles` (user_id, role_id) VALUES
(1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ------------------------------------------------------------
-- 10. RECREATE FOREIGN KEYS AND CONSTRAINTS
-- ------------------------------------------------------------

-- Core RBAC and Users
ALTER TABLE `users` ADD CONSTRAINT `fk_users_departments` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL;
ALTER TABLE `user_roles` ADD CONSTRAINT `fk_ur_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
ALTER TABLE `user_roles` ADD CONSTRAINT `fk_ur_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

-- Groups and Students
ALTER TABLE `groups` ADD CONSTRAINT `fk_groups_students` FOREIGN KEY (`captain_id`) REFERENCES `students` (`id`) ON DELETE SET NULL;
ALTER TABLE `students` ADD CONSTRAINT `fk_students_departments` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL;
ALTER TABLE `students` ADD CONSTRAINT `fk_students_groups` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE SET NULL;

-- Activities Configuration
ALTER TABLE `activity_subgroups` ADD CONSTRAINT `fk_subgroups_stages` FOREIGN KEY (`stage_id`) REFERENCES `activity_stages` (`id`) ON DELETE CASCADE;
ALTER TABLE `activity_subgroups` ADD CONSTRAINT `fk_subgroups_users` FOREIGN KEY (`assigned_faculty_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;
ALTER TABLE `activity_subgroups` ADD CONSTRAINT `fk_subgroups_departments` FOREIGN KEY (`assigned_department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL;
ALTER TABLE `activities` ADD CONSTRAINT `fk_activities_subgroups` FOREIGN KEY (`subgroup_id`) REFERENCES `activity_subgroups` (`id`) ON DELETE CASCADE;

-- Discipline Logs, Attendance, and Marks
ALTER TABLE `discipline_logs` ADD CONSTRAINT `fk_logs_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;
ALTER TABLE `discipline_logs` ADD CONSTRAINT `fk_logs_subgroups` FOREIGN KEY (`subgroup_id`) REFERENCES `activity_subgroups` (`id`) ON DELETE SET NULL;
ALTER TABLE `discipline_logs` ADD CONSTRAINT `fk_logs_users` FOREIGN KEY (`recorded_by_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT;

ALTER TABLE `attendance` ADD CONSTRAINT `fk_attendance_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;
ALTER TABLE `attendance` ADD CONSTRAINT `fk_attendance_users` FOREIGN KEY (`recorded_by_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

ALTER TABLE `marks` ADD CONSTRAINT `fk_marks_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;
ALTER TABLE `marks` ADD CONSTRAINT `fk_marks_subjects` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE;
ALTER TABLE `marks` ADD CONSTRAINT `fk_marks_users` FOREIGN KEY (`recorded_by_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- Audits and History
ALTER TABLE `audit_logs` ADD CONSTRAINT `fk_audits_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;
ALTER TABLE `login_history` ADD CONSTRAINT `fk_login_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ------------------------------------------------------------
-- 11. BUILD PERFORMANCE INDEXES
-- ------------------------------------------------------------

-- Indexes on students
CREATE INDEX `idx_students_score` ON `students` (`score` DESC);
CREATE INDEX `idx_students_cohort` ON `students` (`department_id`, `academic_year`, `section`);
CREATE INDEX `idx_students_name` ON `students` (`full_name`(50));

-- Indexes on transactional tables
CREATE INDEX `idx_fk_discipline_logs_student` ON `discipline_logs` (`student_id`);
CREATE INDEX `idx_fk_attendance_student_date` ON `attendance` (`student_id`, `date`);
CREATE INDEX `idx_fk_marks_student_subject` ON `marks` (`student_id`, `subject_id`);
CREATE INDEX `idx_marks_report` ON `marks` (`subject_id`, `exam_type`);
CREATE INDEX `idx_notifications_recipient` ON `notifications` (`recipient_type`, `recipient_id`, `read_status`);
CREATE INDEX `idx_audits_search` ON `audit_logs` (`action`, `created_at`);

-- ------------------------------------------------------------
-- 12. CLEANUP TEMPORARY SCHEMA TRANSITION COLUMNS
-- ------------------------------------------------------------

ALTER TABLE `user_statuses` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `frequencies` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `cap_periods` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `evidence_types` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `notification_types` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `submission_statuses` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `violation_statuses` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `violation_severities` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `xp_source_types` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `categories` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `permissions` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `departments` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `roles` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `users` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `groups` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `students` DROP COLUMN IF EXISTS `old_user_id`;
ALTER TABLE `rule_conditions` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `submissions` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `submission_evidence` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `violation_types` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `violations` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `violation_evidence` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `xp_transactions` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `streaks` DROP COLUMN IF EXISTS `old_id`;
ALTER TABLE `notifications` DROP COLUMN IF EXISTS `old_id`;

-- Drop deprecated transition tables safely
DROP TABLE IF EXISTS `student_profiles`;
DROP TABLE IF EXISTS `faculty_profiles`;
DROP TABLE IF EXISTS `student_parents`;

-- ------------------------------------------------------------
-- 13. RECREATE DATABASE TRIGGERS
-- ------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_xp_after_insert
AFTER INSERT ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BIGINT DEFAULT 1;
    DECLARE v_vio_id BIGINT DEFAULT 2;
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE cur_total INT;
    DECLARE new_stage BIGINT;

    SET week_start = DATE_SUB(UTC_DATE(), INTERVAL WEEKDAY(UTC_DATE()) DAY);
    SET month_start = DATE_FORMAT(UTC_DATE(), '%Y-%m-01');

    INSERT INTO student_xp_summary (student_id, total_xp, weekly_xp, monthly_xp,
                                    week_start_date, month_start_date,
                                    total_submissions_approved, total_violations)
    VALUES (NEW.student_id, NEW.xp_change, NEW.xp_change, NEW.xp_change,
            week_start, month_start,
            IF(NEW.source_type_id = v_sub_id, 1, 0),
            IF(NEW.source_type_id = v_vio_id, 1, 0))
    ON DUPLICATE KEY UPDATE
        total_xp = total_xp + NEW.xp_change,
        weekly_xp = IF(week_start_date = week_start, weekly_xp + NEW.xp_change, NEW.xp_change),
        monthly_xp = IF(month_start_date = month_start, monthly_xp + NEW.xp_change, NEW.xp_change),
        week_start_date = week_start,
        month_start_date = month_start,
        total_submissions_approved = total_submissions_approved +
            IF(NEW.source_type_id = v_sub_id, 1, 0),
        total_violations = total_violations +
            IF(NEW.source_type_id = v_vio_id, 1, 0);

    SELECT total_xp INTO cur_total FROM student_xp_summary WHERE student_id = NEW.student_id;

    SELECT s.id INTO new_stage
    FROM activity_stages s
    ORDER BY s.id DESC
    LIMIT 1;

    SET new_stage = COALESCE(new_stage, 1);

    INSERT INTO student_stage_progress (student_id, current_stage_id, previous_stage_id,
                                        promoted_at, stage_started_at)
    VALUES (NEW.student_id, new_stage, NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
    ON DUPLICATE KEY UPDATE
        previous_stage_id = IF(current_stage_id != new_stage, current_stage_id, previous_stage_id),
        current_stage_id = new_stage,
        promoted_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), promoted_at),
        stage_started_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), stage_started_at);
END//

CREATE TRIGGER trg_xp_after_update
AFTER UPDATE ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BIGINT DEFAULT 1;
    DECLARE v_vio_id BIGINT DEFAULT 2;
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE delta INT;
    DECLARE cur_total INT;
    DECLARE new_stage BIGINT;

    IF NEW.xp_change != OLD.xp_change THEN
        SET week_start = DATE_SUB(UTC_DATE(), INTERVAL WEEKDAY(UTC_DATE()) DAY);
        SET month_start = DATE_FORMAT(UTC_DATE(), '%Y-%m-01');
        SET delta = NEW.xp_change - OLD.xp_change;

        UPDATE student_xp_summary
        SET total_xp = total_xp + delta,
            weekly_xp = IF(week_start_date = week_start, weekly_xp + delta, delta),
            monthly_xp = IF(month_start_date = month_start, monthly_xp + delta, delta),
            week_start_date = week_start,
            month_start_date = month_start,
            total_submissions_approved = total_submissions_approved +
                IF(NEW.source_type_id = v_sub_id, 1, 0) - IF(OLD.source_type_id = v_sub_id, 1, 0),
            total_violations = total_violations +
                IF(NEW.source_type_id = v_vio_id, 1, 0) - IF(OLD.source_type_id = v_vio_id, 1, 0)
        WHERE student_id = NEW.student_id;

        SELECT total_xp INTO cur_total FROM student_xp_summary WHERE student_id = NEW.student_id;

        SELECT s.id INTO new_stage
        FROM activity_stages s
        ORDER BY s.id DESC
        LIMIT 1;

        SET new_stage = COALESCE(new_stage, 1);

        INSERT INTO student_stage_progress (student_id, current_stage_id, previous_stage_id,
                                            promoted_at, stage_started_at)
        VALUES (NEW.student_id, new_stage, NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE
            previous_stage_id = IF(current_stage_id != new_stage, current_stage_id, previous_stage_id),
            current_stage_id = new_stage,
            promoted_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), promoted_at),
            stage_started_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), stage_started_at);
    END IF;
END//

CREATE TRIGGER trg_xp_after_delete
AFTER DELETE ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BIGINT DEFAULT 1;
    DECLARE v_vio_id BIGINT DEFAULT 2;
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE delta INT;
    DECLARE cur_total INT;
    DECLARE new_stage BIGINT;

    SET week_start = DATE_SUB(UTC_DATE(), INTERVAL WEEKDAY(UTC_DATE()) DAY);
    SET month_start = DATE_FORMAT(UTC_DATE(), '%Y-%m-01');
    SET delta = -OLD.xp_change;

    UPDATE student_xp_summary
    SET total_xp = total_xp + delta,
        weekly_xp = IF(week_start_date = week_start, weekly_xp + delta, delta),
        monthly_xp = IF(month_start_date = month_start, monthly_xp + delta, delta),
        week_start_date = week_start,
        month_start_date = month_start,
        total_submissions_approved = total_submissions_approved - IF(OLD.source_type_id = v_sub_id, 1, 0),
        total_violations = total_violations - IF(OLD.source_type_id = v_vio_id, 1, 0)
    WHERE student_id = OLD.student_id;

    SELECT total_xp INTO cur_total FROM student_xp_summary WHERE student_id = OLD.student_id;

    SELECT s.id INTO new_stage
    FROM activity_stages s
    ORDER BY s.id DESC
    LIMIT 1;

    SET new_stage = COALESCE(new_stage, 1);

    INSERT INTO student_stage_progress (student_id, current_stage_id, previous_stage_id,
                                        promoted_at, stage_started_at)
    VALUES (OLD.student_id, new_stage, NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
    ON DUPLICATE KEY UPDATE
        previous_stage_id = IF(current_stage_id != new_stage, current_stage_id, previous_stage_id),
        current_stage_id = new_stage,
        promoted_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), promoted_at),
        stage_started_at = IF(current_stage_id != new_stage, UTC_TIMESTAMP(6), stage_started_at);
END//

DELIMITER ;

-- Re-enable foreign key validation checks
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- END OF UPGRADE MIGRATION
-- ============================================================
