package jjcet.PragatiX.admin;

import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/debug-activities")
public class DebugRunnerActivities {
    
    @Autowired
    private ActivityRepository activityRepository;

    @GetMapping
    public List<String> debug() {
            return activityRepository.findAll().stream()
                .map(a -> "Activity: " + a.getName() 
                    + " | Stage: " + (a.getStage() != null ? a.getStage().getId() : "null")
                    + " | Subgroup: " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null")
                    + " | AcadYear: " + a.getAcademicYear())
                .collect(Collectors.toList());
    }

    @Autowired
    private jjcet.PragatiX.modules.authentication.repository.UserRepository userRepository;

    @GetMapping("/teacher")
    public String debugTeacher() {
        jjcet.PragatiX.entity.User teacher = userRepository.findByUsername("jaga").orElse(null);
        if (teacher == null) return "Teacher not found";
        return "Teacher: " + teacher.getUsername() 
            + " | Dept: " + (teacher.getDepartment() != null ? teacher.getDepartment().getName() : "null")
            + " | Sec: " + (teacher.getSection() != null ? teacher.getSection().getSectionName() : "null")
            + " | Roles: " + teacher.getSubRoles().stream().map(r -> r.getName()).collect(Collectors.toList());
    }

    @Autowired
    private jjcet.PragatiX.repository.TeamRepository teamRepository;

    @GetMapping("/team/{id}")
    public String debugTeam(@org.springframework.web.bind.annotation.PathVariable Long id) {
        jjcet.PragatiX.entity.Team t = teamRepository.findById(id).orElse(null);
        if (t == null) return "Team not found";
        return "Team: " + t.getName()
            + " | Dept: " + (t.getDepartment() != null ? t.getDepartment().getName() : "null")
            + " | Sec: " + (t.getSection() != null ? t.getSection().getSectionName() : "null")
            + " | Year: " + t.getYear();
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner fixStudentScores(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        return args -> {
            try {
                int updated = jdbc.update("UPDATE students SET score = total_xp WHERE score = total_xp + 100 OR score > total_xp");
                if (updated > 0) {
                    System.out.println("Cleaned up " + updated + " student scores by removing extra 100 offset.");
                }
                jdbc.update("UPDATE levels SET academic_year = 'FIRST_YEAR' WHERE academic_year IS NULL");
                jdbc.update("UPDATE levels SET is_deleted = false WHERE is_deleted IS NULL");
            } catch (Exception e) {
                System.out.println("Could not run fixStudentScores/initLevels: " + e.getMessage());
            }
        };
    }

    @Autowired
    private jjcet.PragatiX.admin.service.TeamValidationService validationService;

    @GetMapping("/test-cc-teams")
    public List<String> testCcTeams() {
        jjcet.PragatiX.entity.User cc = userRepository.findByUsername("jaga").orElse(null);
        if (cc == null) return List.of("CC jaga not found");
        
        List<jjcet.PragatiX.entity.Team> teams = teamRepository.findFilteredTeams("FIRST_YEAR", null, null);
        return teams.stream().map(t -> {
            String name = t.getName();
            boolean passed = false;
            String reason = "OK";
            if (t.getCaptain() == null && (t.getMembers() == null || t.getMembers().isEmpty())) {
                reason = "Empty Team";
            } else {
                try {
                    validationService.validateTeamAccess(cc, t);
                    passed = true;
                } catch (Exception e) {
                    reason = e.getMessage();
                }
            }
            return "Team: " + name + " | Year: " + t.getYear() + " | Dept: " + (t.getDepartment() != null ? t.getDepartment().getName() : "null") + " | Sec: " + (t.getSection() != null ? t.getSection().getSectionName() : "null") + " | Passed: " + passed + " | Reason: " + reason;
        }).collect(Collectors.toList());
    }
}
