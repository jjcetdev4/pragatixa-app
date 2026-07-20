package com.spdms.infrastructure.initializer;

import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.activity.repository.*;
import com.spdms.modules.faculty.repository.*;
import com.spdms.modules.student.repository.*;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.authentication.repository.SubRoleRepository;
import com.spdms.modules.authentication.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Seeds default roles, departments, academic years, sections, users, and students on startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicYearRepository academicYearRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final GenderRepository genderRepository;
    private final SectionRepository sectionRepository;
    private final SubRoleRepository subRoleRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityRepository activityRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder,
                           AcademicYearRepository academicYearRepository,
                           YearRepository yearRepository,
                           SemesterRepository semesterRepository,
                           GenderRepository genderRepository,
                           SectionRepository sectionRepository,
                           SubRoleRepository subRoleRepository,
                           ActivityAssignmentRepository activityAssignmentRepository,
                           ActivityRepository activityRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.academicYearRepository = academicYearRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
        this.genderRepository = genderRepository;
        this.sectionRepository = sectionRepository;
        this.subRoleRepository = subRoleRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAcademicYears();
        seedGenders();
        seedDepartments();
        seedTeacherUser();
        seedAdminUser();

        // Ensure default student has password "1234"
        studentRepository.findByStudentId("sharugesh").ifPresent(s -> {
            String current = s.getPassword();
            if (current == null || !passwordEncoder.matches("1234", current)) {
                s.setPassword(passwordEncoder.encode("1234"));
                studentRepository.save(s);
                log.debug("Reset default student sharugesh password to 1234");
            }
        });

        migrateStudentPasswords();
        migrateActivityXp();
    }

    private void migrateActivityXp() {
        List<Activity> activities = activityRepository.findAll();
        boolean saved = false;
        for (Activity act : activities) {
            if (act.getAwardEnabled() == null || act.getPenaltyEnabled() == null) {
                int legacyXp = act.getAwardXp() != null ? act.getAwardXp() : 0;
                String type = act.getXpType() != null ? act.getXpType() : "Reward";

                if ("Penalty".equalsIgnoreCase(type) || "Discipline".equalsIgnoreCase(type)) {
                    act.setAwardEnabled(false);
                    act.setAwardXp(0);
                    act.setPenaltyEnabled(true);
                    act.setPenaltyXp(legacyXp);
                } else if ("Mixed".equalsIgnoreCase(type)) {
                    act.setAwardEnabled(true);
                    act.setPenaltyEnabled(true);
                    act.setPenaltyXp(legacyXp);
                } else {
                    act.setAwardEnabled(true);
                    act.setAwardXp(legacyXp);
                    act.setPenaltyEnabled(false);
                    act.setPenaltyXp(0);
                }

                activityRepository.save(act);
                saved = true;
            }
        }
        if (saved) {
            log.debug("Migrated existing activities to new Award/Penalty toggle schema successfully.");
        }
    }

    private void seedGenders() {
        for (String name : new String[]{"Male", "Female", "Other"}) {
            if (genderRepository.findByGenderName(name).isEmpty()) {
                genderRepository.save(Gender.builder().genderName(name).build());
                log.debug("Seeded gender: {}", name);
            }
        }
    }

    private void seedRoles() {
        for (String roleName : new String[]{"ROLE_ADMIN", "ROLE_TEACHER", "ROLE_TRANSPORT", "ROLE_STUDENT"}) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.debug("Seeded role: {}", roleName);
            }
        }
    }

    private void seedDepartments() {
        String[][] depts = {
            {"Computer Science and Engineering", "CSE"},
            {"Electronics and Communication", "ECE"},
            {"Mechanical Engineering", "MECH"},
            {"Civil Engineering", "CIVIL"},
            {"Business Administration", "MBA"},
            {"Information Technology", "IT"}
        };
        for (String[] d : depts) {
            if (departmentRepository.findByDeptCode(d[1]).isEmpty()) {
                departmentRepository.save(Department.builder()
                    .deptCode(d[1])
                    .deptName(d[0])
                    .code(d[1])
                    .name(d[0])
                    .description(null)
                    .build());
                log.debug("Seeded department: {}", d[1]);
            }
        }
    }

    private void seedAcademicYears() {
        if (academicYearRepository.findByAcademicYear("2024-2025").isEmpty()) {
            academicYearRepository.save(AcademicYear.builder()
                .academicYear("2024-2025")
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2025, 5, 31))
                .status(AcademicYear.Status.ACTIVE)
                .build());
            log.debug("Seeded academic year: 2024-2025");
        }
    }

    private void seedTeacherUser() {
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER").orElseThrow();
        
        SubRole ccSubrole = subRoleRepository.findByName("CC").orElseGet(() -> 
            subRoleRepository.save(SubRole.builder().name("CC").role(teacherRole).build())
        );
        SubRole hodSubrole = subRoleRepository.findByName("HOD").orElseGet(() -> 
            subRoleRepository.save(SubRole.builder().name("HOD").role(teacherRole).build())
        );

        Department cseDept = departmentRepository.findByDeptCode("CSE").orElse(null);

        userRepository.findByUsername("jaga").ifPresentOrElse(
            existing -> {
                existing.setDepartment(cseDept);
                existing.setYear("1");
                existing.setSection(null);
                userRepository.save(existing);
                log.debug("CC Teacher jaga profile updated with department, year");
            },
            () -> {
                User ccTeacher = User.builder()
                    .username("jaga")
                    .password(passwordEncoder.encode("1234"))
                    .fullName("Jaga CC")
                    .email("jaga@spdms.com")
                    .roles(Set.of(teacherRole))
                    .subRoles(Set.of(ccSubrole))
                    .department(cseDept)
                    .year("1")
                    .section(null)
                    .active(true)
                    .build();
                userRepository.save(ccTeacher);
                log.debug("CC Teacher created: username=jaga | password=1234");
            }
        );

        if (!userRepository.existsByUsername("sharu")) {
            User hodTeacher = User.builder()
                .username("sharu")
                .password(passwordEncoder.encode("1234"))
                .fullName("Sharu HOD")
                .email("sharu@spdms.com")
                .roles(Set.of(teacherRole))
                .subRoles(Set.of(hodSubrole))
                .active(true)
                .build();
            userRepository.save(hodTeacher);
            log.debug("HOD Teacher created: username=sharu | password=1234");
        }
    }

    private void seedStudentUser() {
        if (!studentRepository.existsByStudentId("sharugesh")) {
            Role studentRole = roleRepository.findByName("ROLE_STUDENT").orElseThrow();
            
            // 1. Seed or resolve Student User account
            User studentUser = userRepository.findByUsername("sharugesh").orElseGet(() -> {
                User u = User.builder()
                    .username("sharugesh")
                    .password(passwordEncoder.encode("1234"))
                    .fullName("Sharugesh")
                    .email("sharugesh@spdms.com")
                    .roles(Set.of(studentRole))
                    .active(true)
                    .build();
                return userRepository.save(u);
            });

            // 2. Fetch lookup entities
            Department cseDept = departmentRepository.findByDeptCode("CSE").orElseThrow();
            Gender male = genderRepository.findByGenderName("Male").orElseThrow();
            AcademicYear ay = academicYearRepository.findByAcademicYear("2024-2025").orElseThrow();
            Year y1 = yearRepository.findByYearNo((byte) 1).orElseThrow();
            Semester s1 = semesterRepository.findBySemesterNo((byte) 1).orElseThrow();

            // 3. Seed Student
            Student student = Student.builder()
                .studentId("sharugesh")
                .fullName("Sharugesh")
                .email("sharugesh@spdms.com")
                .password(passwordEncoder.encode("1234"))
                .department(cseDept)
                .section(null)
                .user(studentUser)
                .genderRef(male)
                .phoneNo("1234567890")
                .academicYearRef(ay)
                .yearRef(y1)
                .semesterRef(s1)
                .academicYear("2024-2025")
                .semester("1")
                .year("1")
                .gender("Male")
                .active(true)
                .score(100)
                .build();
            
            studentRepository.save(student);
            log.debug("Default student created: studentId=sharugesh | password=1234");
        }
    }

    private void migrateStudentPasswords() {
        log.debug("Starting student password migration check...");
        java.util.List<Student> students = studentRepository.findAll();
        int count = 0;
        for (Student s : students) {
            String currentPassword = s.getPassword();
            if (currentPassword == null || currentPassword.trim().isEmpty() || !currentPassword.startsWith("$2a$")) {
                LocalDate dob = s.getDateOfBirth();
                String rawPassword = (dob != null)
                    ? dob.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"))
                    : s.getStudentId();
                if (rawPassword != null && !rawPassword.trim().isEmpty()) {
                    s.setPassword(passwordEncoder.encode(rawPassword));
                    studentRepository.save(s);
                    count++;
                }
            }
        }
        if (count > 0) {
            log.debug("Completed student password migration. Updated {} student passwords.", count);
        } else {
            log.debug("All student passwords are up to date.");
        }
    }

    private void seedAdminUser() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        userRepository.findByUsername("admin").ifPresentOrElse(
            existing -> {
                existing.setPassword(passwordEncoder.encode("12345"));
                userRepository.save(existing);
                log.debug("Admin password updated: username=admin | password=12345");
            },
            () -> {
                User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("12345"))
                    .fullName("System Administrator")
                    .email("admin@spdms.com")
                    .roles(Set.of(adminRole))
                    .active(true)
                    .build();
                userRepository.save(admin);
                log.debug("Default admin created: username=admin | password=12345");
            }
        );
    }

}
