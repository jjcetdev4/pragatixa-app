package com.spdms.config;

import com.spdms.entity.Role;
import com.spdms.entity.User;
import com.spdms.modules.authentication.repository.RoleRepository;
import com.spdms.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Component
public class SuperAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking for default Super Admin account...");

        // Ensure ROLE_SUPER_ADMIN exists
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseGet(() -> {
                    log.info("ROLE_SUPER_ADMIN not found. Creating...");
                    Role role = new Role();
                    role.setName("ROLE_SUPER_ADMIN");
                    return roleRepository.save(role);
                });

        Optional<User> existingAdmin = userRepository.findByUsername("admin");

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            boolean hasSuperAdminRole = admin.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"));

            if (!hasSuperAdminRole) {
                log.info("Updating existing 'admin' user to ROLE_SUPER_ADMIN...");
                admin.getRoles().add(superAdminRole);
                userRepository.save(admin);
            } else {
                log.info("Super Admin already exists. Skipping creation.");
            }
        } else {
            log.info("Super Admin not found. Creating default Super Admin...");
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setFullName("System Super Administrator");
            admin.setActive(true);
            admin.setRoles(Collections.singleton(superAdminRole));
            
            userRepository.save(admin);
            log.info("Done.");
        }
    }
}
