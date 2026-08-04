package com.pragatix.modules.authentication.security;

import com.pragatix.entity.Student;
import com.pragatix.entity.User;
import com.pragatix.modules.student.exception.StudentNotFoundException;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAuthResolver {

    private static final Logger log = LoggerFactory.getLogger(StudentAuthResolver.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentAuthResolver(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Resolves the currently authenticated Student identity using stable mapping
     * rather than just email.
     * 1. Looks up the authenticated User.
     * 2. Checks if there is a direct User -> Student relationship (findByUserId).
     * 3. Checks if the Username matches regNo, regNo, or sprNo.
     * 4. As a last resort, checks email matching.
     * 
     * @return The authenticated Student entity
     * @throws StudentNotFoundException if no mapping exists
     */
    @Transactional(readOnly = true)
    public Student getLoggedInStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Student student = null;
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            // 1. Try to find by direct User relationship
            student = studentRepository.findByUserId(user.getId()).orElse(null);
        }

        // 2. Try to find by stable identifiers matching the JWT subject (username)
        if (student == null) {
            student = studentRepository.findByRegNo(username).orElse(null);
        }
        if (student == null) {
            student = studentRepository.findBySprNo(username).orElse(null);
        }

        // 3. Fallback: email matching
        if (student == null && user != null && user.getEmail() != null) {
            student = studentRepository.findByEmail(user.getEmail()).orElse(null);
        }

        if (student == null) {
            log.error("Student resolution failed: No Student profile found for Username '{}', User ID '{}'", username,
                    user.getId());
            throw new StudentNotFoundException("Student profile not found for this user");
        }

        log.debug("Resolved Authenticated Student - Username: {}, Resolved Student ID: {}, Resolved Student Name: {}",
                username, student.getRegNo(), student.getFullName());

        return student;
    }
}
