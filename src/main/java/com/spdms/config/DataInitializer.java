package com.spdms.config;

import com.spdms.entity.*;
import com.spdms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds default roles, departments, and a superadmin user on first startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedDepartments();
        seedAdminUser();
        seedTeacherUser();
    }

    private void seedRoles() {
        for (String roleName : new String[]{"ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT"}) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedDepartments() {
        String[][] depts = {
            {"Computer Science and Engineering", "CSE"},
            {"Electronics and Communication", "ECE"},
            {"Mechanical Engineering", "MECH"},
            {"Civil Engineering", "CIVIL"},
            {"Business Administration", "MBA"}
        };
        for (String[] d : depts) {
            if (departmentRepository.findByCode(d[1]).isEmpty()) {
                departmentRepository.save(Department.builder().name(d[0]).code(d[1]).build());
                log.info("Seeded department: {}", d[1]);
            }
        }
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .email("admin@spdms.com")
                .roles(Set.of(adminRole))
                .active(true)
                .build();
            userRepository.save(admin);
            log.info("Default admin created: username=admin | password=Admin@123");
        }
    }

    private void seedTeacherUser() {
        if (!userRepository.existsByUsername("teacher1")) {
            Role teacherRole = roleRepository.findByName("ROLE_TEACHER").orElseThrow();
            User teacher = User.builder()
                .username("teacher1")
                .password(passwordEncoder.encode("Teacher@123"))
                .fullName("Sample Teacher")
                .email("teacher1@spdms.com")
                .roles(Set.of(teacherRole))
                .active(true)
                .build();
            userRepository.save(teacher);
            log.info("Default teacher created: username=teacher1 | password=Teacher@123");
        }
    }
}
