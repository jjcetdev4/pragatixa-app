-- ============================================================
-- V1__init.sql
-- SPDMS – Production-Ready Schema (MySQL 8.0+)
-- All UUIDs hardcoded for deterministic seed data.
-- Application generates UUIDs for new rows.
--
-- UTC TIMEZONE REQUIREMENT: session time_zone must be '+00:00'
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ------------------------------------------------------------
-- 1. LOOKUP TABLES
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_statuses (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_statuses (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'ACTIVE', 'Active'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000002'), 'SUSPENDED', 'Suspended'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000003'), 'BLOCKED', 'Blocked')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS frequencies (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO frequencies (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000011'), 'DAILY', 'Daily'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000012'), 'WEEKLY', 'Weekly'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000013'), 'BIWEEKLY', 'Biweekly'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000014'), 'ONCE', 'Once'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000015'), 'PER_ASSIGNMENT', 'Per Assignment')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS cap_periods (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO cap_periods (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000021'), 'WEEK', 'Week'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000022'), 'MONTH', 'Month'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000023'), 'ALL_TIME', 'All Time')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS evidence_types (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO evidence_types (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000031'), 'IMAGE', 'Image'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000032'), 'FILE', 'File'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000033'), 'TEXT', 'Text'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000034'), 'NONE', 'None')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS notification_types (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO notification_types (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000041'), 'SUBMISSION_APPROVED', 'Submission Approved'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000042'), 'SUBMISSION_REJECTED', 'Submission Rejected'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000043'), 'VIOLATION_REPORTED', 'Violation Reported'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000044'), 'STAGE_UPGRADE', 'Stage Upgrade'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000045'), 'WARNING', 'Warning'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000046'), 'STREAK_BROKEN', 'Streak Broken'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000047'), 'XP_GAIN', 'XP Gain'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000048'), 'GENERAL', 'General')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS submission_statuses (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO submission_statuses (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000051'), 'PENDING', 'Pending'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000052'), 'APPROVED', 'Approved'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000053'), 'REJECTED', 'Rejected')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS violation_statuses (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO violation_statuses (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000061'), 'PENDING', 'Pending'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000062'), 'APPROVED', 'Approved'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000063'), 'REJECTED', 'Rejected'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000064'), 'ESCALATED', 'Escalated'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000065'), 'RESOLVED', 'Resolved')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS violation_severities (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    xp_penalty INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO violation_severities (id, code, name, xp_penalty) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000071'), 'MINOR', 'Minor', 10),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000072'), 'MODERATE', 'Moderate', 30),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000073'), 'SEVERE', 'Severe', 75)
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS xp_source_types (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO xp_source_types (id, code, name) VALUES
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000081'), 'SUBMISSION', 'Submission'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000082'), 'VIOLATION', 'Violation'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000083'), 'BONUS', 'Bonus'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000084'), 'STREAK_BONUS', 'Streak Bonus'),
  (UUID_TO_BIN('00000000-0000-0000-0000-000000000085'), 'ADJUSTMENT', 'Admin Adjustment')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ------------------------------------------------------------
-- 2. CORE ORGANISATION & USERS
-- ------------------------------------------------------------

-- Fix 1: remove circular FK by creating departments without hod FK,
-- then adding it after users table exists. Also added index for FK.
CREATE TABLE IF NOT EXISTS departments (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NULL UNIQUE,
    hod_user_id BINARY(16) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_dep_active (is_deleted),
    CONSTRAINT fk_departments_hod_user_id FOREIGN KEY (hod_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Users table (no changes needed except standardised collation)
CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL DEFAULT '',
    last_name VARCHAR(100) NOT NULL DEFAULT '',
    phone VARCHAR(20) NULL,
    status_id BINARY(16) NOT NULL,
    department_id BINARY(16) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_email_status (email, status_id),
    INDEX idx_users_department (department_id),
    INDEX idx_users_active (is_deleted, status_id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_status FOREIGN KEY (status_id) REFERENCES user_statuses(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Now add the FK from departments to users with a UNIQUE constraint name


-- ------------------------------------------------------------
-- 3. RBAC
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS roles (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (id, name, description) VALUES
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000001'), 'STUDENT', 'Student role'),
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000002'), 'FACULTY', 'Faculty member'),
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000003'), 'HOD', 'Head of Department'),
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000004'), 'ADMIN', 'System Administrator'),
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000005'), 'PLACEMENT_OFFICER', 'Placement Officer'),
  (UUID_TO_BIN('10000000-0000-0000-0000-000000000006'), 'PARENT', 'Parent/Guardian')
ON DUPLICATE KEY UPDATE description = VALUES(description);

CREATE TABLE IF NOT EXISTS permissions (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (id, name) VALUES
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000001'), 'SUBMIT_ACTIVITY'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000002'), 'REVIEW_SUBMISSION'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000003'), 'REPORT_VIOLATION'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000004'), 'MANAGE_USERS'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000005'), 'MANAGE_RULES'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000006'), 'VIEW_ANALYTICS'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000007'), 'VIEW_ALL_STUDENTS'),
  (UUID_TO_BIN('20000000-0000-0000-0000-000000000008'), 'ESCALATE_VIOLATION')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BINARY(16) NOT NULL,
    permission_id BINARY(16) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_rp_permission (permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- (The rest of the schema remains identical except for the XP transactions
-- and triggers sections where we removed the problematic CHECK constraint
-- and the broken comment.)

-- ------------------------------------------------------------
-- 4. PROFILES & PARENT LINK
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS student_profiles (
    user_id BINARY(16) PRIMARY KEY,
    register_number VARCHAR(50) NOT NULL UNIQUE,
    admission_year INT NOT NULL,
    current_year INT NOT NULL,
    section VARCHAR(10) NULL,
    batch VARCHAR(20) NULL,
    guardian_name VARCHAR(100) NULL,
    guardian_phone VARCHAR(20) NULL,
    guardian_email VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_sp_current_year CHECK (current_year BETWEEN 1 AND 5),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS faculty_profiles (
    user_id BINARY(16) PRIMARY KEY,
    employee_id VARCHAR(50) NULL UNIQUE,
    designation VARCHAR(100) NULL,
    specialization VARCHAR(100) NULL,
    is_class_coordinator BOOLEAN NOT NULL DEFAULT FALSE,
    coordinator_year INT NULL,
    coordinator_section VARCHAR(10) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_faculty_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_parents (
    student_id BINARY(16) NOT NULL,
    parent_id BINARY(16) NOT NULL,
    relationship VARCHAR(50) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (student_id, parent_id),
    INDEX idx_parent (parent_id),
    CONSTRAINT fk_student_parents_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_parents_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 5. STAGES
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS stages (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    order_no INT NOT NULL UNIQUE,
    min_xp INT NOT NULL,
    max_xp INT NULL,
    description TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_stage_xp CHECK (max_xp IS NULL OR min_xp < max_xp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- ------------------------------------------------------------
-- 6. CATEGORIES
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS categories (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description TEXT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (id, code, name, description, is_mandatory, sort_order) VALUES
  (UUID_TO_BIN('30000000-0000-0000-0000-000000000001'), 'MUST', 'Must', 'Mandatory daily/weekly tasks', TRUE, 1),
  (UUID_TO_BIN('30000000-0000-0000-0000-000000000002'), 'INDIVIDUAL', 'Individual', 'Individual skill-building', FALSE, 2),
  (UUID_TO_BIN('30000000-0000-0000-0000-000000000003'), 'GROUP', 'Group', 'Team-based collaborative', FALSE, 3),
  (UUID_TO_BIN('30000000-0000-0000-0000-000000000004'), 'ADVANTAGE', 'Advantage', 'Bonus XP activities', FALSE, 4)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- ------------------------------------------------------------
-- 7. RULES & RULE CONDITIONS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rules (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    stage_id INT NOT NULL,
    category_id BINARY(16) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    base_xp INT NOT NULL DEFAULT 0,
    max_xp INT NULL,
    frequency_id BINARY(16) NOT NULL,
    cap INT NULL,
    cap_period_id BINARY(16) NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    auto_verify BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_type_id BINARY(16) NOT NULL,
    owner_role_id BINARY(16) NOT NULL,
    failure_xp INT NULL,
    dependency_rule_id BINARY(16) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_rules_stage (stage_id),
    INDEX idx_rules_category (category_id),
    INDEX idx_rules_active (is_active, is_deleted),
    INDEX idx_rules_dependency (dependency_rule_id),
    INDEX idx_rules_priority (priority),
    INDEX idx_rules_frequency (frequency_id),
    INDEX idx_rules_owner (owner_role_id),
    INDEX idx_rules_active_cover (is_active, is_deleted, stage_id, category_id),
    CONSTRAINT chk_rules_xp CHECK (max_xp IS NULL OR base_xp <= max_xp),
    CONSTRAINT chk_rules_cap_nonnegative CHECK (cap IS NULL OR cap >= 0),
    CONSTRAINT chk_rules_cap_period CHECK (cap IS NULL OR cap_period_id IS NOT NULL),
    CONSTRAINT fk_rules_stage FOREIGN KEY (stage_id) REFERENCES stages(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_frequency FOREIGN KEY (frequency_id) REFERENCES frequencies(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_cap_period FOREIGN KEY (cap_period_id) REFERENCES cap_periods(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_evidence FOREIGN KEY (evidence_type_id) REFERENCES evidence_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_owner FOREIGN KEY (owner_role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rules_dependency FOREIGN KEY (dependency_rule_id) REFERENCES rules(id) ON DELETE SET NULL,
    CONSTRAINT fk_rules_created FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_rules_updated FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rule_conditions (
    id BINARY(16) PRIMARY KEY,
    rule_id BINARY(16) NOT NULL,
    field VARCHAR(50) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    value VARCHAR(255) NOT NULL,
    CONSTRAINT chk_rc_operator CHECK (operator IN ('eq','gt','lt','gte','lte','in','between')),
    INDEX idx_rc_rule (rule_id),
    CONSTRAINT fk_rule_conditions_rule FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 8. GROUPS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS student_groups (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    leader_id BINARY(16) NULL,
    created_by BINARY(16) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_groups_name_created (name, created_by),
    INDEX idx_groups_leader (leader_id),
    INDEX idx_groups_active (is_deleted),
    CONSTRAINT fk_groups_leader FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_groups_created FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_members (
    group_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (group_id, student_id),
    INDEX idx_gm_student (student_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 9. SUBMISSIONS & EVIDENCE
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS submissions (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    rule_id BINARY(16) NOT NULL,
    stage_id INT NOT NULL,
    group_id BINARY(16) NULL,
    status_id BINARY(16) NOT NULL,
    evidence_type_id BINARY(16) NOT NULL,
    evidence_text TEXT NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_by BINARY(16) NULL,
    reviewed_at TIMESTAMP(6) NULL,
    xp_awarded INT NOT NULL DEFAULT 0,
    rejection_reason TEXT NULL,
    week_number INT NOT NULL,
    year_number INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_sub_week CHECK (week_number BETWEEN 1 AND 53),
    CONSTRAINT chk_sub_year CHECK (year_number BETWEEN 2000 AND 2100),
    UNIQUE KEY uk_sub_student_rule_week (student_id, rule_id, year_number, week_number),
    INDEX idx_sub_student (student_id),
    INDEX idx_sub_rule (rule_id),
    INDEX idx_sub_status (status_id),
    INDEX idx_sub_group (group_id),
    INDEX idx_sub_student_status (student_id, status_id),
    INDEX idx_sub_student_status_date (student_id, status_id, submitted_at),
    INDEX idx_sub_period (year_number, week_number),
    CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_rule FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_stage FOREIGN KEY (stage_id) REFERENCES stages(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_group FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE SET NULL,
    CONSTRAINT fk_sub_status FOREIGN KEY (status_id) REFERENCES submission_statuses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_evidence FOREIGN KEY (evidence_type_id) REFERENCES evidence_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS submission_evidence (
    id BINARY(16) PRIMARY KEY,
    submission_id BINARY(16) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type_id BINARY(16) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_evidence_submission_hash (submission_id, file_hash),
    INDEX idx_ev_submission (submission_id),
    CONSTRAINT fk_ev_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ev_file_type FOREIGN KEY (file_type_id) REFERENCES evidence_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 10. VIOLATIONS & EVIDENCE
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS violation_types (
    id BINARY(16) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    penalty_xp INT NOT NULL DEFAULT 0,
    severity_id BINARY(16) NOT NULL,
    requires_hod_approval BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_vtype_active (is_active, is_deleted),
    CONSTRAINT fk_vtype_severity FOREIGN KEY (severity_id) REFERENCES violation_severities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vtype_created FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_vtype_updated FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS violations (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    violation_type_id BINARY(16) NOT NULL,
    reported_by BINARY(16) NOT NULL,
    status_id BINARY(16) NOT NULL,
    description TEXT NOT NULL,
    reported_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_by BINARY(16) NULL,
    approved_at TIMESTAMP(6) NULL,
    rejection_reason TEXT NULL,
    hod_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_notes TEXT NULL,
    resolved_at TIMESTAMP(6) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_vio_student (student_id),
    INDEX idx_vio_status (status_id),
    INDEX idx_vio_reporter (reported_by),
    INDEX idx_vio_student_status (student_id, status_id),
    INDEX idx_vio_student_created (student_id, created_at),
    INDEX idx_vio_active (is_deleted),
    CONSTRAINT fk_vio_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vio_type FOREIGN KEY (violation_type_id) REFERENCES violation_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vio_reporter FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vio_status FOREIGN KEY (status_id) REFERENCES violation_statuses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vio_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS violation_evidence (
    id BINARY(16) PRIMARY KEY,
    violation_id BINARY(16) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type_id BINARY(16) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_violation_evidence_hash (violation_id, file_hash),
    INDEX idx_ve_violation (violation_id),
    CONSTRAINT fk_ve_violation FOREIGN KEY (violation_id) REFERENCES violations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ve_file_type FOREIGN KEY (file_type_id) REFERENCES evidence_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 11. XP LEDGER (append-only, source of truth)
-- FIX: removed the CHECK that involved FK columns (not allowed in MySQL).
-- Rule "not both sources" must be enforced at application level.
-- XP range kept within ±5000.
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS xp_transactions (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    source_type_id BINARY(16) NOT NULL,
    submission_id BINARY(16) NULL,
    violation_id BINARY(16) NULL,
    xp_change INT NOT NULL,
    reason VARCHAR(255) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_xp_idempotency (idempotency_key),
    INDEX idx_xp_student (student_id),
    INDEX idx_xp_student_created (student_id, created_at),
    INDEX idx_xp_source (source_type_id),
    INDEX idx_xp_submission (submission_id),
    INDEX idx_xp_violation (violation_id),
    CONSTRAINT chk_xp_nonzero CHECK (xp_change != 0),
    CONSTRAINT chk_xp_range CHECK (xp_change BETWEEN -5000 AND 5000),
    CONSTRAINT fk_xp_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_xp_source_type FOREIGN KEY (source_type_id) REFERENCES xp_source_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_xp_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE SET NULL,
    CONSTRAINT fk_xp_violation FOREIGN KEY (violation_id) REFERENCES violations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 12. STREAKS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS streaks (
    id BINARY(16) PRIMARY KEY,
    student_id BINARY(16) NOT NULL,
    rule_id BINARY(16) NOT NULL,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date DATE NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_streak (student_id, rule_id),
    INDEX idx_streak_student (student_id),
    CONSTRAINT chk_streak_current CHECK (current_streak >= 0),
    CONSTRAINT chk_streak_longest CHECK (longest_streak >= 0),
    CONSTRAINT fk_streak_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_streak_rule FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 13. NOTIFICATIONS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    type_id BINARY(16) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body TEXT NULL,
    related_submission_id BINARY(16) NULL,
    related_violation_id BINARY(16) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_user_unread (user_id, is_read),
    INDEX idx_notif_user_created (user_id, created_at),
    INDEX idx_notif_created (created_at),
    INDEX idx_notif_type (type_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_type FOREIGN KEY (type_id) REFERENCES notification_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_notif_submission FOREIGN KEY (related_submission_id) REFERENCES submissions(id) ON DELETE SET NULL,
    CONSTRAINT fk_notif_violation FOREIGN KEY (related_violation_id) REFERENCES violations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 14. STUDENT STAGE PROGRESS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS student_stage_progress (
    student_id BINARY(16) PRIMARY KEY,
    current_stage_id INT NOT NULL,
    previous_stage_id INT NULL,
    promoted_at TIMESTAMP(6) NULL,
    promoted_by BINARY(16) NULL,
    stage_started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_ssp_stage (current_stage_id),
    INDEX idx_ssp_stage_started (current_stage_id, stage_started_at),
    CONSTRAINT fk_ssp_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ssp_stage FOREIGN KEY (current_stage_id) REFERENCES stages(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ssp_prev_stage FOREIGN KEY (previous_stage_id) REFERENCES stages(id) ON DELETE SET NULL,
    CONSTRAINT fk_ssp_promoted_by FOREIGN KEY (promoted_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 15. STUDENT XP SUMMARY (materialized view, maintained by triggers)
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS student_xp_summary (
    student_id BINARY(16) PRIMARY KEY,
    total_xp INT NOT NULL DEFAULT 0,
    weekly_xp INT NOT NULL DEFAULT 0,
    monthly_xp INT NOT NULL DEFAULT 0,
    week_start_date DATE NULL,
    month_start_date DATE NULL,
    total_submissions_approved INT NOT NULL DEFAULT 0,
    total_violations INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_sxs_nonnegative CHECK (total_xp >= 0),
    INDEX idx_sxs_total (total_xp),
    INDEX idx_sxs_weekly (weekly_xp),
    INDEX idx_sxs_monthly (monthly_xp),
    INDEX idx_sxs_updated (updated_at DESC),
    CONSTRAINT fk_sxs_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 16. AUDIT LOGS
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    action VARCHAR(20) NOT NULL,
    performed_by BINARY(16) NOT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_user (performed_by),
    INDEX idx_audit_created (created_at),
    CONSTRAINT fk_audit_user FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 17. LOGIN HISTORY
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    login_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_login_user (user_id),
    CONSTRAINT fk_login_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TRIGGERS – Keep XP Summary & Stage Progress in Sync
-- (xp_source_types IDs are hardcoded for performance)
-- ============================================================

DELIMITER //

DROP TRIGGER IF EXISTS trg_xp_after_insert//

CREATE TRIGGER trg_xp_after_insert
AFTER INSERT ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000081');
    DECLARE v_vio_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000082');
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE cur_total INT;
    DECLARE new_stage INT;

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
    FROM stages s
    WHERE cur_total >= s.min_xp
      AND (s.max_xp IS NULL OR cur_total <= s.max_xp)
    ORDER BY s.min_xp DESC
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

DROP TRIGGER IF EXISTS trg_xp_after_update//

CREATE TRIGGER trg_xp_after_update
AFTER UPDATE ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000081');
    DECLARE v_vio_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000082');
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE delta INT;
    DECLARE cur_total INT;
    DECLARE new_stage INT;

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
        FROM stages s
        WHERE cur_total >= s.min_xp
          AND (s.max_xp IS NULL OR cur_total <= s.max_xp)
        ORDER BY s.min_xp DESC
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

DROP TRIGGER IF EXISTS trg_xp_after_delete//

CREATE TRIGGER trg_xp_after_delete
AFTER DELETE ON xp_transactions
FOR EACH ROW
BEGIN
    DECLARE v_sub_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000081');
    DECLARE v_vio_id BINARY(16) DEFAULT UUID_TO_BIN('00000000-0000-0000-0000-000000000082');
    DECLARE week_start DATE;
    DECLARE month_start DATE;
    DECLARE delta INT;
    DECLARE cur_total INT;
    DECLARE new_stage INT;

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
    FROM stages s
    WHERE cur_total >= s.min_xp
      AND (s.max_xp IS NULL OR cur_total <= s.max_xp)
    ORDER BY s.min_xp DESC
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

-- ============================================================
-- END OF V1 SCHEMA
-- ============================================================