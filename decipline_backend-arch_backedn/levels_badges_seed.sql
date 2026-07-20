-- ========================================================
-- JJCET GAMIFIED STUDENT SYSTEM: LEVELS & BADGES SEED
-- Execute this script in your MySQL Database (spdms_lab)
-- ========================================================

-- 1. Create tables if they do not exist
CREATE TABLE IF NOT EXISTS levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_number INT NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    xp_min INT NOT NULL,
    xp_max INT NOT NULL,
    stage INT NOT NULL,
    primary_objective TEXT,
    key_unlocks TEXT
);

CREATE TABLE IF NOT EXISTS badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    tier VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    xp_required INT NOT NULL,
    icon_url VARCHAR(255),
    approval_authority VARCHAR(100) NOT NULL,
    rarity VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS student_badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    approved_by VARCHAR(100),
    evidence_url TEXT,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE
);

-- 2. Populate Levels (8-Level Progression)
INSERT INTO levels (level_number, title, xp_min, xp_max, stage, primary_objective, key_unlocks)
VALUES
(1, 'Explorer', 0, 100, 1, 'Build participation habits', 'Onboarding missions, basic badges, attend all sessions'),
(2, 'Builder', 101, 500, 1, 'Develop consistency & discipline', 'Study groups, quiz battles, attendance streaks'),
(3, 'Innovator', 501, 1500, 2, 'Build technical & collaborative skills', 'Skill pathways unlocked, mini-projects, peer collaboration'),
(4, 'Specialist', 1501, 3000, 2, 'Demonstrate competency & peer support', 'Advanced missions, certification tracks, own deliverables'),
(5, 'Leader', 3001, 5000, 3, 'Guide peers, lead teams strategically', 'Mentorship roles, leadership missions, project lead'),
(6, 'Mentor', 5001, 7000, 3, 'Sustain ecosystem & peer development', 'Governance participation, ecosystem stewardship'),
(7, 'Architect', 7001, 10000, 3, 'Influence ecosystem growth & innovation', 'Industry opportunities, innovation access, strategic leadership'),
(8, 'Industry Ready', 10001, 99999, 3, 'Professional-level readiness - placement & alumni', 'Full privileges, alumni bridge, institutional ambassador')
ON DUPLICATE KEY UPDATE
title=VALUES(title), xp_min=VALUES(xp_min), xp_max=VALUES(xp_max), stage=VALUES(stage), primary_objective=VALUES(primary_objective), key_unlocks=VALUES(key_unlocks);

-- 3. Populate Badges (5 Tiers)
INSERT INTO badges (name, tier, description, xp_required, icon_url, approval_authority, rarity)
VALUES
-- Foundation (Common)
('Attendance Warrior', 'Foundation', 'Maintain 95% attendance for a full calendar month.', 50, '', 'Faculty', 'Common'),
('Participation Star', 'Foundation', 'Actively participate and answer questions in all class hours for a week.', 40, '', 'Faculty', 'Common'),
('Punctuality Pro', 'Foundation', 'Arrive before the bell rings without any late entries for 2 consecutive weeks.', 30, '', 'Faculty', 'Common'),

-- Achievement (Uncommon)
('Code Ninja', 'Achievement', 'Complete daily coding challenges on C/Python for 15 consecutive days.', 200, '', 'Faculty + Evaluator', 'Uncommon'),
('GPA Master', 'Achievement', 'Score a GPA of 8.5 or higher in the semester examinations.', 300, '', 'Faculty + Evaluator', 'Uncommon'),
('Consistency Champion', 'Achievement', 'Maintain all active daily streaks for 30 consecutive days.', 150, '', 'Faculty + Evaluator', 'Uncommon'),
('Hackathon Finisher', 'Achievement', 'Participate and submit a working project in an internal department hackathon.', 250, '', 'Faculty + Evaluator', 'Uncommon'),

-- Excellence (Rare)
('Full Stack Warrior', 'Excellence', 'Build and host a web application with complete frontend and backend services.', 800, '', 'Program Management', 'Rare'),
('Interview Slayer', 'Excellence', 'Clear the first-round technical mock interviews conducted by internal placement cell.', 600, '', 'Program Management', 'Rare'),
('Internship Achiever', 'Excellence', 'Secure and successfully complete a verified 4-week industry internship.', 1000, '', 'Program Management', 'Rare'),
('Event Commander', 'Excellence', 'Lead and organize a technical/non-technical program or seminar in the college.', 500, '', 'Program Management', 'Rare'),

-- Elite (Very Rare)
('Team Captain Badge', 'Elite', 'Serve as a team captain and lead the group to an Elite status (4500+ XP).', 1500, '', 'Governance Council', 'Very Rare'),
('Mentor Hero', 'Elite', 'Conduct peer teaching and mentor at least 5 junior students to improve their grades.', 1200, '', 'Governance Council', 'Very Rare'),
('Research Pioneer', 'Elite', 'Submit a research paper draft accepted/reviewed by the department committee.', 2000, '', 'Governance Council', 'Very Rare'),
('Innovation Catalyst', 'Elite', 'Develop a working prototype in the CoE/D2P Lab validated by an industry mentor.', 1800, '', 'Governance Council', 'Very Rare'),

-- Legacy (Legendary)
('Startup Builder', 'Legacy', 'Create a viable project proposal incubated or registered as a student startup.', 3500, '', 'Dean / Principal', 'Legendary'),
('Placement Champion', 'Legacy', 'Get placed in a tier-1 company with a package exceeding threshold limit.', 3000, '', 'Dean / Principal', 'Legendary'),
('JJCET Legend', 'Legacy', 'Reach a lifetime cumulative score of 3500+ XP points.', 3500, '', 'Dean / Principal', 'Legendary'),
('Alumni Pioneer', 'Legacy', 'Act as institutional ambassador and secure industry linkage / MoUs for college.', 4000, '', 'Dean / Principal', 'Legendary')
ON DUPLICATE KEY UPDATE
tier=VALUES(tier), description=VALUES(description), xp_required=VALUES(xp_required), approval_authority=VALUES(approval_authority), rarity=VALUES(rarity);
