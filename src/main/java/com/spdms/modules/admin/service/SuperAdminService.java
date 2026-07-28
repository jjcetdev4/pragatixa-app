package com.spdms.modules.admin.service;

import com.spdms.entity.AssignedAcademicYear;
import com.spdms.entity.Role;
import com.spdms.entity.User;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.authentication.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllYearAdmins() {
        return userRepository.findAllByRoleName("ROLE_ADMIN").stream()
                .filter(u -> u.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN")))
                .collect(Collectors.toList());
    }

    @Transactional
    public User createYearAdmin(String fullName, String username, String password, String email, String phone, AssignedAcademicYear assignedAcademicYear, boolean active) {
        // Validation: Only one active Admin per Academic Year
        if (active) {
            deactivateExistingYearAdmins(assignedAcademicYear);
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        User newAdmin = User.builder()
                .fullName(fullName)
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .phone(phone)
                .assignedAcademicYear(assignedAcademicYear)
                .active(active)
                .roles(java.util.Set.of(adminRole))
                .build();

        return userRepository.save(newAdmin);
    }

    @Transactional
    public User updateYearAdmin(Long id, String fullName, String username, String password, String email, String phone, AssignedAcademicYear assignedAcademicYear, boolean active) {
        User existingAdmin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Year Admin not found with ID: " + id));

        if (existingAdmin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("Super Admin cannot be edited through Year Admin Management.");
        }

        if (active && (existingAdmin.getAssignedAcademicYear() != assignedAcademicYear || !existingAdmin.isActive())) {
            deactivateExistingYearAdmins(assignedAcademicYear);
        }

        existingAdmin.setFullName(fullName);
        if (username != null && !username.trim().isEmpty()) {
            existingAdmin.setUsername(username.trim());
        }
        if (password != null && !password.isEmpty()) {
            existingAdmin.setPassword(passwordEncoder.encode(password));
        }
        existingAdmin.setEmail(email);
        existingAdmin.setPhone(phone);
        existingAdmin.setAssignedAcademicYear(assignedAcademicYear);
        existingAdmin.setActive(active);

        return userRepository.save(existingAdmin);
    }

    @Transactional
    public void deleteYearAdmin(Long id, String loggedInUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Year Admin not found with ID: " + id));

        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("Super Admin cannot be deleted.");
        }

        if (user.getUsername().equals(loggedInUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }

        userRepository.delete(user);
    }

    private void deactivateExistingYearAdmins(AssignedAcademicYear academicYear) {
        List<User> activeAdminsForYear = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .filter(u -> u.getAssignedAcademicYear() == academicYear)
                .filter(User::isActive)
                .collect(Collectors.toList());

        for (User admin : activeAdminsForYear) {
            admin.setActive(false);
            userRepository.save(admin);
        }
    }
}
