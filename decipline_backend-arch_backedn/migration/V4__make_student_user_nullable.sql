-- Migration to make user_id nullable on students table
ALTER TABLE `students` MODIFY COLUMN `user_id` bigint NULL;
