package jjcet.PragatiX.config;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DepartmentMasterDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentMasterDataSeeder.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentMasterDataSeeder(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking and seeding required missing departments...");

        List<DepartmentConfig> requiredDepartments = Arrays.asList(
                new DepartmentConfig("Aeronautical Engineering", "AERO", true),
                new DepartmentConfig("Mechanical Engineering", "MECH", true),
                new DepartmentConfig("Civil Engineering", "CIVIL", true),
                new DepartmentConfig("Computer Science and Engineering", "CSE", true),
                new DepartmentConfig("Computer Science and Engineering (Cyber Security)", "CS", true),
                new DepartmentConfig("Electrical and Electronics Engineering", "EEE", true),
                new DepartmentConfig("Electronics and Communication Engineering", "ECE", true),
                new DepartmentConfig("Information Technology", "IT", true),
                new DepartmentConfig("Artificial Intelligence and Data Science", "AIDS", true),
                new DepartmentConfig("Department of Tamil", "TAMIL", false),
                new DepartmentConfig("Department of Chemistry", "CHEM", false),
                new DepartmentConfig("Department of Mathematics", "MATH", false),
                new DepartmentConfig("Department of English", "ENG", false),
                new DepartmentConfig("Department of Physics", "PHY", false)
        );

        List<Department> existingDepts = departmentRepository.findAll();

        for (DepartmentConfig req : requiredDepartments) {
            String trimmedName = req.name.trim();

            boolean exists = existingDepts.stream()
                    .anyMatch(d -> (d.getName() != null && d.getName().trim().equalsIgnoreCase(trimmedName))
                                || (d.getDeptName() != null && d.getDeptName().trim().equalsIgnoreCase(trimmedName)));

            if (!exists) {
                String codeToUse = req.code;
                String finalCodeToUse = codeToUse;
                boolean codeExists = existingDepts.stream()
                        .anyMatch(d -> (d.getCode() != null && d.getCode().trim().equalsIgnoreCase(finalCodeToUse))
                                    || (d.getDeptCode() != null && d.getDeptCode().trim().equalsIgnoreCase(finalCodeToUse)));

                if (codeExists) {
                    codeToUse = codeToUse + "_1";
                }

                Department newDept = Department.builder()
                        .name(trimmedName)
                        .deptName(trimmedName)
                        .code(codeToUse)
                        .deptCode(codeToUse)
                        .supportsSections(req.supportsSections)
                        .build();

                departmentRepository.save(newDept);
                logger.info("Created department {}.", trimmedName);
            }
        }
        for (Department existing : existingDepts) {
            String exName = existing.getName() != null ? existing.getName().trim() : (existing.getDeptName() != null ? existing.getDeptName().trim() : "");
            boolean isRequired = requiredDepartments.stream().anyMatch(req -> req.name.equalsIgnoreCase(exName));
            
            if (!isRequired) {
                existing.setDeleted(true);
                departmentRepository.save(existing);
                logger.info("Removed unnecessary department: {}", exName);
            } else {
                boolean reqSupports = requiredDepartments.stream()
                        .filter(req -> req.name.equalsIgnoreCase(exName))
                        .findFirst()
                        .map(req -> req.supportsSections)
                        .orElse(true);
                existing.setSupportsSections(reqSupports);
                existing.setDeleted(false);
                departmentRepository.save(existing);
            }
        }
    }

    private static class DepartmentConfig {
        String name;
        String code;
        boolean supportsSections;

        DepartmentConfig(String name, String code, boolean supportsSections) {
            this.name = name;
            this.code = code;
            this.supportsSections = supportsSections;
        }
    }
}
