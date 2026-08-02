ALTER TABLE attendance_settings
ADD COLUMN start_date DATE,
ADD COLUMN end_date DATE;

ALTER TABLE attendance_settings
DROP COLUMN week_start_day,
DROP COLUMN week_end_day;
