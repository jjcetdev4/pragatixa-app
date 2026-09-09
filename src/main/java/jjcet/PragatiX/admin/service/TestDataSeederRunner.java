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
    private final OtpTokenRepository otpTokenRepository;

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
            SectionRepository sectionRepository,
            OtpTokenRepository otpTokenRepository) {
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
        this.otpTokenRepository = otpTokenRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=================================================================");
        log.info("ACCOUNT SEEDER & CLEANUP: Starting execution...");
        log.info("=================================================================");

        try {
            // 1. Remove all old test accounts where username, fullName, or email starts with "test" or "jjcetpm"
            try {
                List<User> allUsers = userRepository.findAll();
                for (User u : allUsers) {
                    String uName = u.getUsername() != null ? u.getUsername().trim().toLowerCase() : "";
                    String fName = u.getFullName() != null ? u.getFullName().trim().toLowerCase() : "";
                    String email = u.getEmail() != null ? u.getEmail().trim().toLowerCase() : "";
                    if (uName.startsWith("test") || fName.startsWith("test") || email.startsWith("test")
                            || uName.startsWith("jjcetpm") || fName.startsWith("jjcetpm") || email.startsWith("jjcetpm")) {
                        log.info("Removing test user: id={}, username={}, email={}", u.getId(), u.getUsername(), u.getEmail());
                        if (u.getEmail() != null) {
                            otpTokenRepository.deleteByEmail(u.getEmail());
                        }
                        userRepository.delete(u);
                    }
                }
            } catch (Exception e) {
                log.warn("Error cleaning up test users: {}", e.getMessage());
            }

            try {
                List<Student> allStudents = studentRepository.findAll();
                for (Student s : allStudents) {
                    String regNo = s.getRegNo() != null ? s.getRegNo().trim().toLowerCase() : "";
                    String fName = s.getFullName() != null ? s.getFullName().trim().toLowerCase() : "";
                    String email = s.getEmail() != null ? s.getEmail().trim().toLowerCase() : "";
                    if (regNo.startsWith("test") || fName.startsWith("test") || email.startsWith("test")
                            || regNo.startsWith("jjcetpm") || fName.startsWith("jjcetpm") || email.startsWith("jjcetpm")) {
                        log.info("Removing test student: id={}, regNo={}, email={}", s.getId(), s.getRegNo(), s.getEmail());
                        if (s.getEmail() != null) {
                            otpTokenRepository.deleteByEmail(s.getEmail());
                        }
                        studentRepository.delete(s);
                    }
                }
            } catch (Exception e) {
                log.warn("Error cleaning up test students: {}", e.getMessage());
            }

            // 2. Resolve Lookup Data
            Department dept = getOrCreateDepartment();
            Section sec = getOrCreateSection(dept);
            Gender gen = getOrCreateGender();
            AcademicYear ay = getOrCreateAcademicYear();
            Year yr = getOrCreateYear();
            Semester sem = getOrCreateSemester();

            // 3. Resolve Roles
            Role superAdminRole = getOrCreateRole("ROLE_SUPER_ADMIN");
            Role adminRole = getOrCreateRole("ROLE_ADMIN");
            Role teacherRole = getOrCreateRole("ROLE_TEACHER");
            Role hodRole = getOrCreateRole("ROLE_HOD");
            Role studentRole = getOrCreateRole("ROLE_STUDENT");

            // Resolve SubRoles
            SubRole hodSubRole = getOrCreateSubRole("HOD", teacherRole);
            SubRole ccSubRole = getOrCreateSubRole("CC", teacherRole);
            SubRole facultySubRole = getOrCreateSubRole("FACULTY", teacherRole);

            // 4. Seed SuperAdmin: superadmin@gmail.com
            seedUser("superadmin@gmail.com", "superadmin", "Super Admin",
                    Set.of(superAdminRole), new HashSet<>(),
                    dept, null, null, null, null);

            // 5. Seed Admin: admin@gmail.com
            seedUser("admin@gmail.com", "admin", "Admin",
                    Set.of(adminRole), new HashSet<>(),
                    dept, sec, "1st Year", yr, jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // 6. Seed HOD: hod@gmail.com
            seedUser("hod@gmail.com", "hod", "Head of Department",
                    Set.of(teacherRole, hodRole), Set.of(hodSubRole),
                    dept, sec, "1st Year", yr, jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // 7. Seed CC (Class Coordinator): cc@gmail.com
            seedUser("cc@gmail.com", "cc", "Class Coordinator",
                    Set.of(teacherRole), Set.of(ccSubRole),
                    dept, sec, "1st Year", yr, jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // 8. Seed Faculty: faculty@gmail.com
            seedUser("faculty@gmail.com", "faculty", "Faculty",
                    Set.of(teacherRole), Set.of(facultySubRole),
                    dept, sec, "1st Year", yr, jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

            // 9. Seed Student: student@gmail.com
            String defaultHashedPassword = passwordEncoder.encode("1234");
            seedStudent("student@gmail.com", "student", "spr_student",
                    "Student", defaultHashedPassword, dept, sec, gen, ay, yr, sem, false);

            // 10. Seed default OTP (1234) for all standard email accounts
            List<String> defaultEmails = List.of(
                    "superadmin@gmail.com",
                    "admin@gmail.com",
                    "hod@gmail.com",
                    "cc@gmail.com",
                    "faculty@gmail.com",
                    "student@gmail.com"
            );
            for (String email : defaultEmails) {
                otpTokenRepository.deleteByEmail(email);
                OtpToken otpToken = new OtpToken(email, "1234", LocalDateTime.now().plusYears(1));
                otpTokenRepository.save(otpToken);
            }

            log.info("ACCOUNT SEEDER: All accounts and default OTPs initialized successfully.");
        } catch (Exception e) {
            log.error("ACCOUNT SEEDER: Failed during seeding execution", e);
        }
        log.info("=================================================================");
    }

    private User seedUser(String email, String username, String fullName, Set<Role> roles,
            Set<SubRole> subRoles, Department dept, Section sec, String year, Year assignedYear, jjcet.PragatiX.enums.AcademicYear academicYear) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        if (user == null) {
            user = new User();
        }
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRoles(roles);
        user.setSubRoles(subRoles);
        user.setDepartment(dept);
        user.setSection(sec);
        user.setYear(year);
        user.setAssignedYear(assignedYear);
        user.setAcademicYear(academicYear);
        user.setPhone("9876543210");
        user.setActive(true);
        user.setDeleted(false);
        User saved = userRepository.save(user);
        log.info("Seeded/Updated User: {} ({}) with roles: {} and subroles: {}", fullName, email, roles, subRoles);
        return saved;
    }

    private Student seedStudent(String email, String regNo, String sprNo, String fullName, String password,
            Department dept, Section sec, Gender gen, AcademicYear ay, Year yr, Semester sem, boolean isCaptain) {
        Student s = studentRepository.findByEmail(email).orElse(null);
        if (s == null) {
            s = studentRepository.findByRegNo(regNo).orElse(null);
        }
        if (s == null) {
            s = new Student();
        }
        s.setEmail(email);
        s.setRegNo(regNo);
        s.setSprNo(sprNo);
        s.setFullName(fullName);
        s.setDepartment(dept);
        s.setSection(sec);
        s.setGenderRef(gen);
        s.setYearRef(yr);
        s.setSemesterRef(sem);
        s.setPhoneNo("9876543210");
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
        for (Year y : list) {
            if (y.getYearNo() == 1 || "1st Year".equalsIgnoreCase(y.getYearName())) {
                return y;
            }
        }
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
        for (Semester s : list) {
            if (s.getSemesterNo() == 1 || "Semester 1".equalsIgnoreCase(s.getSemesterName())) {
                return s;
            }
        }
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
            if ("Alpha Team".equals(t.getName())) {
                return t;
            }
        }
        Team t = new Team();
        t.setName("Alpha Team");
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
