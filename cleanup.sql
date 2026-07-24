SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM stage_teams WHERE team_id IN (SELECT id FROM teams WHERE id NOT IN (SELECT team_id FROM students WHERE team_id IS NOT NULL));
DELETE FROM teams WHERE id NOT IN (SELECT team_id FROM students WHERE team_id IS NOT NULL);
DELETE FROM stage_teams WHERE team_id NOT IN (SELECT id FROM teams);
DELETE t1 FROM stage_teams t1 INNER JOIN stage_teams t2 WHERE t1.id > t2.id AND t1.stage_id = t2.stage_id AND t1.team_id = t2.team_id;
UPDATE stage_teams st JOIN teams t ON st.team_id = t.id SET st.captain_id = t.captain_id;
SET FOREIGN_KEY_CHECKS = 1;
