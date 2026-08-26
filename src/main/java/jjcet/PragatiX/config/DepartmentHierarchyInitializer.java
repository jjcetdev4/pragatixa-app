package jjcet.PragatiX.config;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.enums.DepartmentType;
import jjcet.PragatiX.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Order(1)
public class DepartmentHierarchyInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DepartmentHierarchyInitializer.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentHierarchyInitializer(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public static class CanonicalDeptDef {
        public final String name;
        public final String defaultCode;
        public final DepartmentType type;
        public final boolean supportsSections;
        public final List<String> aliases;

        public CanonicalDeptDef(String name, String defaultCode, DepartmentType type, boolean supportsSections, String... aliases) {
            this.name = name;
            this.defaultCode = defaultCode;
            this.type = type;
            this.supportsSections = supportsSections;
            this.aliases = Arrays.asList(aliases);
        }
    }

    public static final List<CanonicalDeptDef> CANONICAL_DEPARTMENTS = Arrays.asList(
            // 9 MAIN DEPARTMENTS
            new CanonicalDeptDef("Aeronautical Engineering", "AERO", DepartmentType.MAIN, true, "AERO", "AE"),
            new CanonicalDeptDef("Mechanical Engineering", "MECH", DepartmentType.MAIN, true, "MECH", "ME"),
            new CanonicalDeptDef("Civil Engineering", "CIVIL", DepartmentType.MAIN, true, "CIVIL", "CE"),
            new CanonicalDeptDef("Computer Science and Engineering", "CSE", DepartmentType.MAIN, true, "CSE", "CS"),
            new CanonicalDeptDef("Computer Science and Engineering (Cyber Security)", "CS_CYBER", DepartmentType.MAIN, true, "CS_CYBER", "CSE_CS", "CYBER SECURITY", "CSE (CYBER SECURITY)"),
            new CanonicalDeptDef("Electrical and Electronics Engineering", "EEE", DepartmentType.MAIN, true, "EEE", "EE"),
            new CanonicalDeptDef("Electronics and Communication Engineering", "ECE", DepartmentType.MAIN, true, "ECE", "EC"),
            new CanonicalDeptDef("Information Technology", "IT", DepartmentType.MAIN, true, "IT"),
            new CanonicalDeptDef("Artificial Intelligence and Data Science", "AIDS", DepartmentType.MAIN, true, "AIDS", "AI_DS", "AI&DS", "AI & DS"),

            // 5 SUB DEPARTMENTS
            new CanonicalDeptDef("Department of Tamil", "TAMIL", DepartmentType.SUB, false, "TAMIL", "TAM"),
            new CanonicalDeptDef("Department of Chemistry", "CHEM", DepartmentType.SUB, false, "CHEM", "CHY", "CHEMISTRY"),
            new CanonicalDeptDef("Department of Mathematics", "MATH", DepartmentType.SUB, false, "MATH", "MAT", "MATHEMATICS", "MATHS"),
            new CanonicalDeptDef("Department of English", "ENG", DepartmentType.SUB, false, "ENG", "ENGLISH"),
            new CanonicalDeptDef("Department of Physics", "PHY", DepartmentType.SUB, false, "PHY", "PHYSICS")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.debug("Starting Department Hierarchy Initialization & Synchronization...");

        List<Department> existingDepts = departmentRepository.findAll();
        Set<Long> processedDeptIds = new HashSet<>();

        for (CanonicalDeptDef def : CANONICAL_DEPARTMENTS) {
            Department matched = findMatchingDepartment(def, existingDepts, processedDeptIds);

            if (matched != null) {
                processedDeptIds.add(matched.getId());
                boolean updated = false;

                if (matched.getDepartmentType() == null || matched.getDepartmentType() != def.type) {
                    matched.setDepartmentType(def.type);
                    updated = true;
                }
                if (matched.getSupportsSections() == null || matched.getSupportsSections() != def.supportsSections) {
                    matched.setSupportsSections(def.supportsSections);
                    updated = true;
                }
                if (matched.getCode() == null || matched.getCode().trim().isEmpty()) {
                    matched.setCode(def.defaultCode);
                    updated = true;
                }
                if (matched.getDeptCode() == null || matched.getDeptCode().trim().isEmpty()) {
                    matched.setDeptCode(def.defaultCode);
                    updated = true;
                }
                if (matched.getName() == null || matched.getName().trim().isEmpty()) {
                    matched.setName(def.name);
                    updated = true;
                }
                if (matched.getDeptName() == null || matched.getDeptName().trim().isEmpty()) {
                    matched.setDeptName(def.name);
                    updated = true;
                }

                if (updated) {
                    departmentRepository.save(matched);
                    log.debug("Updated canonical department [ID: {}, Name: '{}', Code: '{}', Type: {}]",
                            matched.getId(), matched.getName(), matched.getCode(), matched.getDepartmentType());
                }
            } else {
                // Create missing canonical department
                Department newDept = Department.builder()
                        .name(def.name)
                        .deptName(def.name)
                        .code(def.defaultCode)
                        .deptCode(def.defaultCode)
                        .departmentType(def.type)
                        .supportsSections(def.supportsSections)
                        .build();

                Department saved = departmentRepository.save(newDept);
                processedDeptIds.add(saved.getId());
                log.debug("Seeded canonical department [ID: {}, Name: '{}', Code: '{}', Type: {}]",
                        saved.getId(), saved.getName(), saved.getCode(), saved.getDepartmentType());
            }
        }

        // Process any remaining non-canonical departments
        for (Department d : existingDepts) {
            if (!processedDeptIds.contains(d.getId()) && !d.isDeleted()) {
                String dName = d.getName() != null ? d.getName() : (d.getDeptName() != null ? d.getDeptName() : "");
                boolean isSub = isSubDepartmentName(dName);
                DepartmentType expectedType = isSub ? DepartmentType.SUB : DepartmentType.MAIN;
                boolean expectedSupportsSections = !isSub;

                if (d.getDepartmentType() == null || d.getDepartmentType() != expectedType
                        || d.getSupportsSections() == null || d.getSupportsSections() != expectedSupportsSections) {
                    d.setDepartmentType(expectedType);
                    d.setSupportsSections(expectedSupportsSections);
                    departmentRepository.save(d);
                    log.debug("Normalized other department [ID: {}, Name: '{}', Type: {}]", d.getId(), d.getName(), expectedType);
                }
            }
        }

        log.debug("Department Hierarchy Initialization completed successfully.");
    }

    private Department findMatchingDepartment(CanonicalDeptDef def, List<Department> existingDepts, Set<Long> processedIds) {
        // 1. Exact name match
        for (Department d : existingDepts) {
            if (processedIds.contains(d.getId())) continue;
            if (d.getName() != null && d.getName().trim().equalsIgnoreCase(def.name)) {
                return d;
            }
            if (d.getDeptName() != null && d.getDeptName().trim().equalsIgnoreCase(def.name)) {
                return d;
            }
        }

        // 2. Normalized name match
        String normDef = normalizeName(def.name);
        for (Department d : existingDepts) {
            if (processedIds.contains(d.getId())) continue;
            if (normalizeName(d.getName()).equals(normDef) || normalizeName(d.getDeptName()).equals(normDef)) {
                return d;
            }
        }

        // 3. Alias / Code match
        for (Department d : existingDepts) {
            if (processedIds.contains(d.getId())) continue;
            String code = d.getCode() != null ? d.getCode().trim().toUpperCase() : "";
            String deptCode = d.getDeptCode() != null ? d.getDeptCode().trim().toUpperCase() : "";

            if (code.equals(def.defaultCode) || deptCode.equals(def.defaultCode)) {
                return d;
            }
            for (String alias : def.aliases) {
                if (code.equalsIgnoreCase(alias) || deptCode.equalsIgnoreCase(alias)
                        || normalizeName(d.getName()).contains(normalizeName(alias))) {
                    return d;
                }
            }
        }

        return null;
    }

    private boolean isSubDepartmentName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase().trim();
        return lower.contains("tamil") || lower.contains("chemistry") || lower.contains("math")
                || lower.contains("english") || lower.contains("physics");
    }

    private String normalizeName(String str) {
        if (str == null) return "";
        return str.toLowerCase()
                .replaceAll("department\\s+of\\s+", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}
