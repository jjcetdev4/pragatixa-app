package com.spdms.security;

import com.spdms.entity.Student;
import com.spdms.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dedicated UserDetailsService for Student authentication.
 * Searches ONLY the students table.
 */
@Service
public class StudentDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(StudentDetailsService.class);
    private final StudentRepository studentRepository;

    public StudentDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[StudentDetailsService] Loading student details for identifier: {}", username);
        
        java.util.Optional<Student> studentOpt = studentRepository.findByStudentId(username)
            .or(() -> studentRepository.findByEmail(username))
            .or(() -> studentRepository.findBySprNo(username));

        if (studentOpt.isEmpty()) {
            try {
                Long regNo = Long.parseLong(username.trim());
                studentOpt = studentRepository.findByRegNo(regNo);
            } catch (NumberFormatException e) {
                // Ignore if not a valid number
            }
        }

        Student student = studentOpt.orElseThrow(() -> {
            log.warn("[StudentDetailsService] Student not found with identifier: {}", username);
            return new UsernameNotFoundException("Student not found with identifier: " + username);
        });

        log.info("[StudentDetailsService] Found student: student_id={}, active={}", student.getStudentId(), student.isActive());

        return User.builder()
            .username(student.getStudentId())
            .password(student.getPassword())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!student.isActive())
            .build();
    }
}
