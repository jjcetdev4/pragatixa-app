
-- Clean up any residual test accounts
DELETE FROM user_sub_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test%' OR username = 'cc_test' OR full_name LIKE 'Test%');
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test%' OR username = 'cc_test' OR full_name LIKE 'Test%');
DELETE FROM users WHERE username LIKE 'test%' OR username = 'cc_test' OR full_name LIKE 'Test%';

