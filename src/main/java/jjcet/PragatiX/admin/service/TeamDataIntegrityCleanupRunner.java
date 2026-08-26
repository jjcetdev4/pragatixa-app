package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.StageTeam;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.repository.TeamRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamDataIntegrityCleanupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeamDataIntegrityCleanupRunner.class);

    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final StudentRepository studentRepository;
    private final TeamCleanupService teamCleanupService;
    private final CaptainSelectionService captainSelectionService;
    private final JdbcTemplate jdbcTemplate;

    public TeamDataIntegrityCleanupRunner(TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository,
            StudentRepository studentRepository,
            TeamCleanupService teamCleanupService,
            CaptainSelectionService captainSelectionService,
            JdbcTemplate jdbcTemplate) {
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.studentRepository = studentRepository;
        this.teamCleanupService = teamCleanupService;
        this.captainSelectionService = captainSelectionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.debug("TEAM DATA INTEGRITY AUDIT & REPAIR: Starting validation check...");

        int repairedCaptains = 0;
        int repairedViceCaptains = 0;
        int repairedStageCaptains = 0;
        int repairedStageViceCaptains = 0;
        int deletedEmptyTeams = 0;

        // 0. Schema Maintenance via JdbcTemplate (Outside JPA Transaction)
        try {
            if (jdbcTemplate != null) {
                // Drop legacy single-column unique index on teams.name if present
                try {
                    List<String> singleColIndexes = jdbcTemplate.query(
                            "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                                    "WHERE TABLE_SCHEMA = DATABASE() " +
                                    "AND TABLE_NAME = 'teams' " +
                                    "AND COLUMN_NAME = 'name' " +
                                    "AND NON_UNIQUE = 0 " +
                                    "AND INDEX_NAME != 'PRIMARY' " +
                                    "AND INDEX_NAME != 'uk_team_name_class' " +
                                    "GROUP BY INDEX_NAME " +
                                    "HAVING COUNT(*) = 1",
                            (rs, rowNum) -> rs.getString("INDEX_NAME"));

                    for (String idxName : singleColIndexes) {
                        log.info("INTEGRITY FIX: Dropping legacy global unique index on teams.name: {}", idxName);
                        try {
                            jdbcTemplate.execute("ALTER TABLE teams DROP INDEX `" + idxName + "`");
                            log.info("Successfully dropped index {}", idxName);
                        } catch (Exception ex) {
                            log.warn("Could not drop index {}: {}", idxName, ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.debug("INFORMATION_SCHEMA inspection for teams skipped/failed: {}", ex.getMessage());
                }

                // Ensure composite unique constraint uk_team_name_class exists on teams
                try {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teams' AND INDEX_NAME = 'uk_team_name_class'",
                            Integer.class);
                    if (count == null || count == 0) {
                        jdbcTemplate.execute(
                                "ALTER TABLE teams ADD CONSTRAINT uk_team_name_class UNIQUE (name, department_id, year, section_id)");
                        log.info(
                                "Successfully added composite unique constraint uk_team_name_class on teams(name, department_id, year, section_id)");
                    }
                } catch (Exception ex) {
                    log.debug("Composite constraint check skipped/failed: {}", ex.getMessage());
                }

                // Ensure team_members table legacy columns are dropped to avoid Error 1364
                try {
                    // 1. Find all foreign keys on reg_no
                    List<String> fkNames = jdbcTemplate.query(
                            "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'team_members' AND COLUMN_NAME = 'reg_no' AND REFERENCED_TABLE_NAME IS NOT NULL",
                            (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));
                    for (String fk : fkNames) {
                        try {
                            jdbcTemplate.execute("ALTER TABLE team_members DROP FOREIGN KEY `" + fk + "`");
                            log.info("INTEGRITY FIX: Dropped legacy foreign key {} on team_members.reg_no", fk);
                        } catch (Exception e) {
                            log.warn("Could not drop foreign key {}: {}", fk, e.getMessage());
                        }
                    }

                    // 2. Drop the reg_no column entirely
                    jdbcTemplate.execute("ALTER TABLE team_members DROP COLUMN reg_no");
                    log.info("INTEGRITY FIX: Dropped legacy column team_members.reg_no to avoid Error 1364.");
                } catch (Exception ex) {
                    log.debug("team_members reg_no column drop skipped or already dropped: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error during teams schema maintenance: {}", e.getMessage());
        }

        // 1. Audit and repair Team entities
        List<Team> allTeams = teamRepository.findAllWithMembers();
        for (Team team : allTeams) {
            boolean teamModified = false;

            // Audit Team Captain
            if (team.getCaptain() != null) {
                Student cap = team.getCaptain();
                if (cap.getTeam() == null || !cap.getTeam().getId().equals(team.getId())) {
                    log.warn(
                            "INTEGRITY FIX: Student {} ({}) is marked as Captain of Team '{}' (ID: {}), but student's active team is '{}' (ID: {}). Unlinking stale captaincy.",
                            cap.getFullName(), cap.getRegNo(), team.getName(), team.getId(),
                            cap.getTeam() != null ? cap.getTeam().getName() : "NONE",
                            cap.getTeam() != null ? cap.getTeam().getId() : null);
                    team.setCaptain(null);
                    teamModified = true;
                    repairedCaptains++;
                }
            }

            // Audit Team Vice Captain
            if (team.getViceCaptain() != null) {
                Student vc = team.getViceCaptain();
                if (vc.getTeam() == null || !vc.getTeam().getId().equals(team.getId())) {
                    log.warn(
                            "INTEGRITY FIX: Student {} ({}) is marked as Vice Captain of Team '{}' (ID: {}), but student's active team is '{}' (ID: {}). Unlinking stale vice-captaincy.",
                            vc.getFullName(), vc.getRegNo(), team.getName(), team.getId(),
                            vc.getTeam() != null ? vc.getTeam().getName() : "NONE",
                            vc.getTeam() != null ? vc.getTeam().getId() : null);
                    team.setViceCaptain(null);
                    teamModified = true;
                    repairedViceCaptains++;
                }
            }

            if (teamModified) {
                teamRepository.save(team);
            }
        }

        // 2. Audit and repair StageTeam entities
        List<StageTeam> allStageTeams = stageTeamRepository.findAll();
        for (StageTeam st : allStageTeams) {
            boolean stModified = false;

            if (st.getCaptain() != null) {
                Student cap = st.getCaptain();
                if (cap.getTeam() == null || !cap.getTeam().getId().equals(st.getTeam().getId())) {
                    log.warn(
                            "INTEGRITY FIX: Student {} ({}) is marked as Stage Captain for Team '{}' in Stage '{}', but student's active team is '{}'. Unlinking stale stage captaincy.",
                            cap.getFullName(), cap.getRegNo(), st.getTeam().getName(),
                            st.getStage() != null ? st.getStage().getStageName() : "N/A",
                            cap.getTeam() != null ? cap.getTeam().getName() : "NONE");
                    st.setCaptain(null);
                    stModified = true;
                    repairedStageCaptains++;
                }
            }

            if (st.getViceCaptain() != null) {
                Student vc = st.getViceCaptain();
                if (vc.getTeam() == null || !vc.getTeam().getId().equals(st.getTeam().getId())) {
                    log.warn(
                            "INTEGRITY FIX: Student {} ({}) is marked as Stage Vice Captain for Team '{}' in Stage '{}', but student's active team is '{}'. Unlinking stale stage vice-captaincy.",
                            vc.getFullName(), vc.getRegNo(), st.getTeam().getName(),
                            st.getStage() != null ? st.getStage().getStageName() : "N/A",
                            vc.getTeam() != null ? vc.getTeam().getName() : "NONE");
                    st.setViceCaptain(null);
                    stModified = true;
                    repairedStageViceCaptains++;
                }
            }

            if (stModified) {
                stageTeamRepository.save(st);
            }
        }

        // 3. Clean up and synchronize team_members table if present
        try {
            if (jdbcTemplate != null) {
                // Delete rows from team_members where the student does not belong to that team
                int removedJoinRows = jdbcTemplate.update(
                        "DELETE tm FROM team_members tm " +
                                "LEFT JOIN students s ON s.id = tm.student_id " +
                                "WHERE s.id IS NULL OR s.team_id IS NULL OR s.team_id != tm.team_id");

                if (removedJoinRows > 0) {
                    log.warn("INTEGRITY FIX: Removed {} stale/duplicate row(s) from team_members table.",
                            removedJoinRows);
                }

                // Ensure every student with a team has a row in team_members
                jdbcTemplate.update(
                        "INSERT IGNORE INTO team_members (team_id, student_id) " +
                                "SELECT s.team_id, s.id FROM students s WHERE s.team_id IS NOT NULL");
            }
        } catch (Exception e) {
            log.debug("team_members table sync skipped or not present: {}", e.getMessage());
        }

        // 4. Auto-delete empty teams & re-evaluate leadership for active teams
        List<Team> remainingTeams = teamRepository.findAllWithMembers();
        for (Team team : remainingTeams) {
            if (teamCleanupService.autoDeleteEmptyTeam(team)) {
                deletedEmptyTeams++;
            } else {
                captainSelectionService.evaluateCaptainForTeam(team);
            }
        }

        log.debug("TEAM DATA INTEGRITY AUDIT & REPAIR COMPLETED: Repaired Captains: {}, Stage Captains: {}, Cleaned Empty Teams: {}",
                repairedCaptains, repairedStageCaptains, deletedEmptyTeams);
    }
}
