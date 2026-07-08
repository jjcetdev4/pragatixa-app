-- Migration to enforce password is never null on students table
ALTER TABLE `students` MODIFY COLUMN `password` varchar(255) NOT NULL;
