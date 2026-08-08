-- ============================================================
-- V20__enforce_team_membership_integrity.sql
-- SPDMS - Enforce One Student = One Team Database Safeguards
-- ============================================================

-- 1. Create or safeguard team_members join table with UNIQUE(student_id) constraint
CREATE TABLE IF NOT EXISTS team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_team_members_student UNIQUE (student_id),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Clean up any invalid/stale captain references in teams table
-- A captain MUST belong to the team (student.team_id == team.id)
UPDATE teams t
LEFT JOIN students s ON s.id = t.captain_id
SET t.captain_id = NULL
WHERE t.captain_id IS NOT NULL 
  AND (s.id IS NULL OR s.team_id IS NULL OR s.team_id != t.id);

-- 3. Clean up any invalid/stale vice-captain references in teams table
-- A vice-captain MUST belong to the team (student.team_id == team.id)
UPDATE teams t
LEFT JOIN students s ON s.id = t.vice_captain_id
SET t.vice_captain_id = NULL
WHERE t.vice_captain_id IS NOT NULL 
  AND (s.id IS NULL OR s.team_id IS NULL OR s.team_id != t.id);

-- 4. Clean up any invalid/stale captain references in stage_teams table
UPDATE stage_teams st
LEFT JOIN students s ON s.id = st.captain_id
SET st.captain_id = NULL
WHERE st.captain_id IS NOT NULL 
  AND (s.id IS NULL OR s.team_id IS NULL OR s.team_id != st.team_id);

-- 5. Clean up any invalid/stale vice-captain references in stage_teams table
UPDATE stage_teams st
LEFT JOIN students s ON s.id = st.vice_captain_id
SET st.vice_captain_id = NULL
WHERE st.vice_captain_id IS NOT NULL 
  AND (s.id IS NULL OR s.team_id IS NULL OR s.team_id != st.team_id);
