-- Migration to make section_id nullable on students table
ALTER TABLE `students` MODIFY COLUMN `section_id` bigint NULL;
