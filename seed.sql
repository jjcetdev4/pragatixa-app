
-- 1. Update test3 (HOD)
UPDATE users 
SET department_id = 14, section_id = 75, year = '1st Year', academic_year = 'FIRST_YEAR', active = 1, deleted = 0
WHERE id = 123 OR email = 'ENC:ezllDfAjVWVO1PHkF0RtzTScXgjuFBkOAzNKefyncB2dvy/5BZlaLjlxlw==';

-- Ensure test3 has ROLE_TEACHER (2) and ROLE_HOD (5)
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (123, 2), (123, 5);
-- Ensure test3 has SubRole HOD (2)
INSERT IGNORE INTO user_sub_roles (user_id, sub_role_id) VALUES (123, 2);

-- 2. Insert or update test4 (CC)
DELETE FROM users WHERE username = 'cc_test' OR email = 'ENC:B1tBM5q9OR+4z7qTZO5a5FjpjAwOqnxMe0xCePuZv026d7b0VoiakhopFw==';
INSERT INTO users (username, password, full_name, email, department_id, section_id, year, academic_year, active, deleted)
VALUES ('cc_test', '$2a$10$eQIlccf5dtdu3jURZ80MK.Mc7gkL4sMgJ.8aW.1xDdUYMKJL7Fdqy', 'Test Class Coordinator', 'ENC:B1tBM5q9OR+4z7qTZO5a5FjpjAwOqnxMe0xCePuZv026d7b0VoiakhopFw==', 14, 75, '1st Year', 'FIRST_YEAR', 1, 0);

SET @cc_id = LAST_INSERT_ID();
-- Ensure test4 has ROLE_TEACHER (2)
INSERT INTO user_roles (user_id, role_id) VALUES (@cc_id, 2);
-- Ensure test4 has SubRole CC (1)
INSERT INTO user_sub_roles (user_id, sub_role_id) VALUES (@cc_id, 1);
