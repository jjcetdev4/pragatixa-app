package com.pragatix.modules.authentication.security;

import com.pragatix.entity.Student;
import com.pragatix.modules.student.repository.StudentRepository;
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
        log.debug("[StudentDetailsService] Loading student details for identifier: {}", username);

        java.util.Optional<Student> studentOpt = studentRepository.findByRegNo(username)
                .or(() -> studentRepository.findByEmail(username))
                .or(() -> studentRepository.findBySprNo(username));

        Student student = studentOpt.orElseThrow(() -> {
            log.warn("[StudentDetailsService] Student not found with identifier: {}", username);
            return new UsernameNotFoundException("Student not found with identifier: " + username);
        });

        log.debug("[StudentDetailsService] Found student: reg_no={}, active={}", student.getRegNo(),
                student.isActive());

        return User.builder()
                .username(student.getRegNo())
                .password(student.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!student.isActive())
                .build();
    }
}
