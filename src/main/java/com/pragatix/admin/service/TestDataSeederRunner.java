package com.pragatix.admin.service;

import com.pragatix.entity.*;
import com.pragatix.repository.*;
import com.pragatix.modules.authentication.repository.*;
import com.pragatix.modules.student.repository.StudentRepository;
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

            // 4. Seed Users
            String defaultHashedPassword = passwordEncoder.encode("password");

            // Super Admin: test1@gmail.com
            seedUser("test1@gmail.com", "superadmin_test", "Test Super Admin", defaultHashedPassword, Set.of(superAdminRole), Set.of(), dept, sec);

            // Admin: test2@gmail.com
            seedUser("test2@gmail.com", "admin_test", "Test Admin", defaultHashedPassword, Set.of(adminRole), Set.of(), dept, sec);

            // HOD: test3@gmail.com
            seedUser("test3@gmail.com", "hod_test", "Test HOD", defaultHashedPassword, Set.of(hodRole), Set.of(), dept, sec);

            // CC: test4@gmail.com
            seedUser("test4@gmail.com", "cc_test", "Test Class Coordinator", defaultHashedPassword, Set.of(teacherRole), Set.of(ccSubRole), dept, sec);

            // Teacher: test5@gmail.com
            seedUser("test5@gmail.com", "teacher_test", "Test Teacher", defaultHashedPassword, Set.of(teacherRole), Set.of(), dept, sec);

            // 5. Seed Students & Team
            Student captain = seedStudent("test6@gmail.com", "test6", "spr6", "Test Captain", defaultHashedPassword, dept, sec, gen, ay, yr, sem, true);
            Student viceCaptain = seedStudent("test7@gmail.com", "test7", "spr7", "Test Vice Captain", defaultHashedPassword, dept, sec, gen, ay, yr, sem, false);
            Student member = seedStudent("test8@gmail.com", "test8", "spr8", "Test Member", defaultHashedPassword, dept, sec, gen, ay, yr, sem, false);

            // Resolve Team
            User teamCreator = userRepository.findByEmail("test4@gmail.com").orElse(null);
            Team team = getOrCreateTeam(captain, viceCaptain, dept, sec, teamCreator);

            // Associate team with students
            if (captain.getTeam() == null) {
                captain.setTeam(team);
                studentRepository.save(captain);
            }
            if (viceCaptain.getTeam() == null) {
                viceCaptain.setTeam(team);
                studentRepository.save(viceCaptain);
            }
            if (member.getTeam() == null) {
                member.setTeam(team);
                studentRepository.save(member);
            }

            log.info("TEST DATA SEEDER: Seeding completed successfully!");
        } catch (Exception e) {
            log.error("TEST DATA SEEDER: Failed to seed test users", e);
        }
        log.info("=================================================================");
    }

    private void seedUser(String email, String username, String fullName, String password, Set<Role> roles, Set<SubRole> subRoles, Department dept, Section sec) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.debug("User already exists: {}", email);
            return;
        }
        User user = User.builder()
                .email(email)
                .username(username)
                .fullName(fullName)
                .password(password)
                .roles(roles)
                .subRoles(subRoles)
                .department(dept)
                .section(sec)
                .active(true)
                .build();
        userRepository.save(user);
        log.info("Seeded User: {} ({})", fullName, email);
    }

    private Student seedStudent(String email, String regNo, String sprNo, String fullName, String password, Department dept, Section sec, Gender gen, AcademicYear ay, Year yr, Semester sem, boolean isCaptain) {
        Optional<Student> existing = studentRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.debug("Student already exists: {}", email);
            return existing.get();
        }
        Student s = new Student();
        s.setEmail(email);
        s.setRegNo(regNo);
        s.setSprNo(sprNo);
        s.setFullName(fullName);
        s.setPassword(password);
        s.setDepartment(dept);
        s.setSection(sec);
        s.setGenderRef(gen);
        s.setAcademicYearRef(ay);
        s.setYearRef(yr);
        s.setSemesterRef(sem);
        s.setPhoneNo("1234567890");
        s.setCaptain(isCaptain);
        s.setActive(true);
        s.setScore(100);
        s.setTotalXp(0);
        s.setGroupXp(0);
        s.setIndividualXp(0);
        s.setMustXp(0);
        s.setStage(1);
        s.setCurrentStage(1);
        Student saved = studentRepository.save(s);
        log.info("Seeded Student: {} ({})", fullName, email);
        return saved;
    }

    private Department getOrCreateDepartment() {
        List<Department> list = departmentRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Department d = new Department();
        d.setDeptCode("TEST");
        d.setDeptName("Test Department");
        d.setName("Test Department");
        return departmentRepository.save(d);
    }

    private Section getOrCreateSection(Department dept) {
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
        if (r.isPresent()) return r.get();

        String rawName = name.replace("ROLE_", "");
        Optional<Role> rRaw = roleRepository.findByName(rawName);
        if (rRaw.isPresent()) return rRaw.get();

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
