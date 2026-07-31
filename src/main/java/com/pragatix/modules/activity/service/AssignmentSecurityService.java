package com.pragatix.modules.activity.service;

import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssignmentSecurityService {

    private final UserRepository userRepository;

    public AssignmentSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Determines whether the given User is the assigned faculty for the given
     * ActivityAssignment.
     * Admin users always return true.
     */
    public boolean isUserAssignedFaculty(ActivityAssignment assignment, User user) {
        if (assignment == null || user == null) {
            return false;
        }

        // 1. Admin Override
        if (user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))) {
            return true;
        }

        // 2. Global Scope (Any Teacher can view/approve)
        if (assignment.getAssignmentScope() == AssignmentScope.GLOBAL) {
            return true;
        }

        // 3. Specific Faculty Scope
        if (assignment.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) {
            return assignment.getTeacher() != null && assignment.getTeacher().getId().equals(user.getId());
        }

        // 4. Department / Section Scope (Class Coordinator resolution)
        if (assignment.getAssignmentScope() == AssignmentScope.DEPARTMENT
                || assignment.getAssignmentScope() == AssignmentScope.SECTION) {
            // Find all CCs for the assigned department and section
            if (assignment.getDepartment() != null && assignment.getSection() != null) {
                List<User> classCoordinators = userRepository.findClassCoordinatorsByDepartmentAndSection(
                        assignment.getDepartment().getId(),
                        assignment.getSection().getId());

                // If the logged-in user is one of the resolved CCs for this specific dept/sec,
                // they are authorized
                return classCoordinators.stream().anyMatch(cc -> cc.getId().equals(user.getId()));
            }
        }

        return false;
    }
}
