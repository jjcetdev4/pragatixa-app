package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.authentication.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class TestDataSeederRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeederRunner.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final RoleRepository roleRepository;
    private final SubRoleRepository subRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final GenderRepository genderRepository;
    private final AcademicYearRepository academicYearRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;

    public TestDataSeederRunner(UserRepository userRepository,
            StudentRepository studentRepository,
            RoleRepository roleRepository,
            SubRoleRepository subRoleRepository,
            PasswordEncoder passwordEncoder,
            DepartmentRepository departmentRepository,
            GenderRepository genderRepository,
            AcademicYearRepository academicYearRepository,
            YearRepository yearRepository,
            SemesterRepository semesterRepository,
            TeamRepository teamRepository,
            SectionRepository sectionRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.roleRepository = roleRepository;
        this.subRoleRepository = subRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
        this.genderRepository = genderRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=================================================================");
        log.info("TEST DATA SEEDER: Starting execution...");
        log.info("=================================================================");

        try {
            // 1. Resolve Lookup Data
            Department dept = getOrCreateDepartment();
            Section sec = getOrCreateSection(dept);
            Gender gen = getOrCreateGender();
            AcademicYear ay = getOrCreateAcademicYear();
            Year yr = getOrCreateYear();
            Semester sem = getOrCreateSemester();

            // 2. Resolve Roles
            Role superAdminRole = getOrCreateRole("ROLE_SUPER_ADMIN");
            Role adminRole = getOrCreateRole("ROLE_ADMIN");
            Role teacherRole = getOrCreateRole("ROLE_TEACHER");
            Role hodRole = getOrCreateRole("ROLE_HOD");
            Role studentRole = getOrCreateRole("ROLE_STUDENT");

            // 3. Resolve SubRoles
            SubRole ccSubRole = getOrCreateSubRole("CC", teacherRole);
            SubRole hodSubRole = getOrCreateSubRole("HOD", teacherRole);

            // 4. Seed Target Users from Credentials list
            String defaultHashedPassword = passwordEncoder.encode("1234");

            // Super Admin: superadmin@gmail.com
            seedUser("superadmin@gmail.com", "superadmin", "Super Admin", defaultHashedPassword,
                    Set.of(superAdminRole), Set.of(), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // Admin: admin@gmail.com
            seedUser("admin@gmail.com", "admin", "Admin", defaultHashedPassword,
                    Set.of(adminRole), Set.of(), dept, sec, "First Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // HOD: hod@gmail.com
            seedUser("hod@gmail.com", "hod", "HOD", defaultHashedPassword,
                    Set.of(teacherRole, hodRole), Set.of(hodSubRole), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // CC: cc@gmail.com
            seedUser("cc@gmail.com", "cc", "Class Coordinator", defaultHashedPassword,
                    Set.of(teacherRole), Set.of(ccSubRole), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // Teacher: teacher@gmail.com
            seedUser("teacher@gmail.com", "teacher", "Teacher", defaultHashedPassword,
                    Set.of(teacherRole), Set.of(), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // Student: student@gmail.com
            seedStudent("student@gmail.com", "STD2026001", "SPR2026001", "Student", defaultHashedPassword,
                    dept, sec, gen, ay, yr, sem, false);

            log.info("TEST DATA SEEDER: Seeding completed successfully for all 6 target accounts!");
        } catch (Exception e) {
            log.error("TEST DATA SEEDER: Failed to seed test users", e);
        }
        log.info("=================================================================");
    }

    private void seedUser(String email, String username, String fullName, String password, Set<Role> roles,
            Set<SubRole> subRoles, Department dept, Section sec, String year, jjcet.PragatiX.enums.AcademicYear academicYear) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(username);
        }
        user.setFullName(fullName);
        user.setRoles(roles != null ? new java.util.HashSet<>(roles) : new java.util.HashSet<>());
        user.setSubRoles(subRoles != null ? new java.util.HashSet<>(subRoles) : new java.util.HashSet<>());
        user.setDepartment(dept);
        user.setSection(sec);
        user.setYear(year);
        user.setAcademicYear(academicYear);
        user.setActive(true);
        user.setDeleted(false);
        userRepository.save(user);
        log.info("Seeded/Updated User: {} ({}) with roles: {} and subroles: {}", fullName, email, roles, subRoles);
    }

    private Student seedStudent(String email, String regNo, String sprNo, String fullName, String password,
            Department dept, Section sec, Gender gen, AcademicYear ay, Year yr, Semester sem, boolean isCaptain) {
        Student s = studentRepository.findByEmail(email).orElse(null);
        if (s == null) {
            s = studentRepository.findByRegNo(regNo).orElse(null);
        }
        if (s == null) {
            s = new Student();
            s.setEmail(email);
            s.setRegNo(regNo);
            s.setSprNo(sprNo);
        }
        s.setFullName(fullName);
        s.setDepartment(dept);
        s.setSection(sec);
        s.setGenderRef(gen);
        s.setYearRef(yr);
        s.setSemesterRef(sem);
        s.setPhoneNo("1234567890");
        s.setCaptain(isCaptain);
        s.setActive(true);
        s.setDeleted(false);
        if (s.getTotalXp() == 0) {
            s.setScore(0);
            s.setTotalXp(0);
            s.setGroupXp(0);
            s.setIndividualXp(0);
            s.setMustXp(0);
            s.setStage(1);
            s.setCurrentStage(1);
        }
        Student saved = studentRepository.save(s);
        log.info("Seeded/Updated Student: {} ({})", fullName, email);
        return saved;
    }

    private Department getOrCreateDepartment() {
        List<Department> list = departmentRepository.findAll();
        for (Department d : list) {
            if ("Information Technology".equalsIgnoreCase(d.getName()) || "IT".equalsIgnoreCase(d.getDeptCode())
                    || "Computer Science and Engineering".equalsIgnoreCase(d.getName()) || "CSE".equalsIgnoreCase(d.getDeptCode())) {
                return d;
            }
        }
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Department d = new Department();
        d.setDeptCode("IT");
        d.setDeptName("Information Technology");
        d.setName("Information Technology");
        return departmentRepository.save(d);
    }

    private Section getOrCreateSection(Department dept) {
        if (dept != null && dept.getId() != null) {
            List<Section> list = sectionRepository.findByDepartment_Id(dept.getId());
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        List<Section> list = sectionRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Section s = new Section();
        s.setSectionName("A");
        s.setDepartment(dept);
        return sectionRepository.save(s);
    }

    private Gender getOrCreateGender() {
        List<Gender> list = genderRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Gender g = new Gender();
        g.setGenderName("Male");
        return genderRepository.save(g);
    }

    private AcademicYear getOrCreateAcademicYear() {
        List<AcademicYear> list = academicYearRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        AcademicYear ay = new AcademicYear();
        ay.setAcademicYear("2026-2027");
        ay.setStartDate(LocalDate.now());
        ay.setEndDate(LocalDate.now().plusYears(1));
        ay.setStatus(AcademicYear.Status.ACTIVE);
        return academicYearRepository.save(ay);
    }

    private Year getOrCreateYear() {
        List<Year> list = yearRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Year y = new Year();
        y.setYearNo((byte) 1);
        y.setYearName("1st Year");
        return yearRepository.save(y);
    }

    private Semester getOrCreateSemester() {
        List<Semester> list = semesterRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Semester sem = new Semester();
        sem.setSemesterNo((byte) 1);
        sem.setSemesterName("Semester 1");
        return semesterRepository.save(sem);
    }

    private Role getOrCreateRole(String name) {
        Optional<Role> r = roleRepository.findByName(name);
        if (r.isPresent())
            return r.get();

        String rawName = name.replace("ROLE_", "");
        Optional<Role> rRaw = roleRepository.findByName(rawName);
        if (rRaw.isPresent())
            return rRaw.get();

        Role newRole = new Role();
        newRole.setName(name);
        return roleRepository.save(newRole);
    }

    private SubRole getOrCreateSubRole(String name, Role parentRole) {
        return subRoleRepository.findByName(name)
                .orElseGet(() -> {
                    SubRole sr = new SubRole();
                    sr.setName(name);
                    sr.setRole(parentRole);
                    return subRoleRepository.save(sr);
                });
    }

    private Team getOrCreateTeam(Student captain, Student viceCaptain, Department dept, Section sec, User createdBy) {
        List<Team> list = teamRepository.findAll();
        for (Team t : list) {
            if ("Test Team".equals(t.getName())) {
                return t;
            }
        }
        Team t = new Team();
        t.setName("Test Team");
        t.setSize(3);
        t.setCaptain(captain);
        t.setViceCaptain(viceCaptain);
        t.setDepartment(dept);
        t.setYear("1st Year");
        t.setSection(sec);
        t.setCreatedBy(createdBy);
        return teamRepository.save(t);
    }
}
