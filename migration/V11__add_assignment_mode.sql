-- V11: Add assignment_mode column to activities table
-- Supports three modes: MANUAL (default), GLOBAL, CLASS_COORDINATOR

ALTER TABLE activities
    ADD COLUMN assignment_mode VARCHAR(50) NOT NULL DEFAULT 'MANUAL';
