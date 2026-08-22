package jjcet.PragatiX.admin;

import jjcet.PragatiX.admin.service.TeamValidationService;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class DebugController {
    
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamValidationService teamValidationService;

    @GetMapping("/api/v1/debug-teams")
    public String debugTeams() {
        StringBuilder sb = new StringBuilder();
        User cc = userRepository.findByUsername("EEE_CC").orElse(null);
        if (cc == null) return "CC not found";
        
        sb.append("CC Section: ").append(cc.getSection() == null ? "null" : cc.getSection().getId()).append("\n");
        sb.append("CC Dept: ").append(cc.getDepartment() == null ? "null" : cc.getDepartment().getId()).append("\n");
        
        List<Team> teams = teamRepository.findAll();
        for (Team t : teams) {
            sb.append("Team: ").append(t.getName()).append(" - ");
            sb.append("Dept: ").append(t.getDepartment() == null ? "null" : t.getDepartment().getId()).append(" - ");
            sb.append("Sec: ").append(t.getSection() == null ? "null" : t.getSection().getId()).append(" - ");
            
            try {
                boolean result = teamValidationService.validateTeamAccess(cc, t);
                sb.append("Access: ").append(result).append("\n");
            } catch (Exception e) {
                sb.append("Access: Exception ").append(e.getClass().getName()).append("\n");
            }
        }
        return sb.toString();
    }
}
