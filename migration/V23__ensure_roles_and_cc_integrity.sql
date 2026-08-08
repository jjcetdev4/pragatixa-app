-- Ensure all expected canonical roles exist in the roles table
INSERT INTO roles (id, name, description) VALUES
  (UUID_TO_BIN(UUID()), 'STUDENT', 'Student role'),
  (UUID_TO_BIN(UUID()), 'FACULTY', 'Faculty member'),
  (UUID_TO_BIN(UUID()), 'TEACHER', 'Teacher role'),
  (UUID_TO_BIN(UUID()), 'HOD', 'Head of Department'),
  (UUID_TO_BIN(UUID()), 'ADMIN', 'System Administrator'),
  (UUID_TO_BIN(UUID()), 'SUPER_ADMIN', 'Super Administrator'),
  (UUID_TO_BIN(UUID()), 'SUPERADMIN', 'Super Administrator'),
  (UUID_TO_BIN(UUID()), 'PLACEMENT_OFFICER', 'Placement Officer'),
  (UUID_TO_BIN(UUID()), 'PARENT', 'Parent/Guardian')
ON DUPLICATE KEY UPDATE description = VALUES(description);
