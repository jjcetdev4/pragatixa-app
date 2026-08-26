package jjcet.PragatiX.admin;

import jjcet.PragatiX.admin.service.TeamValidationService;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.repository.TeamRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

//@Component
public class DebugRunner {
    
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private StudentRepository studentRepository;

    @PostConstruct
    public void runDebug() {
        System.out.println("================== DEBUG STUDENT 315 & TEAMS ==================");
        try {
            Student student = studentRepository.findById(315L).orElse(null);
            
            String canonical = null;
            Long deptId = null;
            Long secId = null;
            String yearStr = null;
            
            if (student == null) {
                System.out.println("Student 315 not found!");
            } else {
                System.out.println("Student 315:");
                System.out.println("  Name: " + student.getFullName());
                System.out.println("  Dept: " + (student.getDepartment() != null ? student.getDepartment().getId() : "null"));
                System.out.println("  Sec : " + (student.getSection() != null ? student.getSection().getId() : "null"));
                System.out.println("  Year: " + student.getYear());
                System.out.println("  AcadYear: " + student.getAcademicYear());
                System.out.println("  AcadYearRef: " + (student.getAcademicYearRef() != null ? student.getAcademicYearRef().getAcademicYear() : "null"));
                
                yearStr = student.getYear();
                System.out.println("  -> Derived YearStr (for count): " + yearStr);
                
                canonical = student.getAcademicYearRef() != null && student.getAcademicYearRef().getAcademicYear() != null && !student.getAcademicYearRef().getAcademicYear().trim().isEmpty() ? student.getAcademicYearRef().getAcademicYear() : (student.getAcademicYear() != null && !student.getAcademicYear().trim().isEmpty() ? student.getAcademicYear() : ("1".equals(yearStr) ? "FIRST_YEAR" : "2".equals(yearStr) ? "SECOND_YEAR" : "3".equals(yearStr) ? "THIRD_YEAR" : "4".equals(yearStr) ? "FOURTH_YEAR" : yearStr));
                System.out.println("  -> Canonical Year: " + canonical);
                
                deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
                secId = student.getSection() != null ? student.getSection().getId() : null;
                
                int countCanonical = teamRepository.countStage1TeamsForClass(deptId, canonical, secId);
                int countRaw = teamRepository.countStage1TeamsForClass(deptId, yearStr, secId);
                
                System.out.println("  -> countStage1TeamsForClass(canonical): " + countCanonical);
                System.out.println("  -> countStage1TeamsForClass(raw): " + countRaw);
            }
            
        try {
            System.out.println("All Stage 1 Teams in DB:");
            List<Object[]> allTeams = teamRepository.findAllStage1TeamsRaw();
            for (Object[] t : allTeams) {
                System.out.println("Team ID: " + t[0] + " | Name: " + t[1] + " | Dept: " + t[2] + " | Sec: " + t[3] + " | Year: " + t[4]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("==================================================================");
    }
}
