package jjcet.PragatiX.modules.activity.service;

import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.ActivityTemporaryAssignment;
import jjcet.PragatiX.entity.AssignmentScope;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.repository.ActivityTemporaryAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssignmentSecurityService {

    private final UserRepository userRepository;
    private final ActivityTemporaryAssignmentRepository temporaryAssignmentRepository;

    public AssignmentSecurityService(UserRepository userRepository,
            ActivityTemporaryAssignmentRepository temporaryAssignmentRepository) {
        this.userRepository = userRepository;
        this.temporaryAssignmentRepository = temporaryAssignmentRepository;
    }

    /**
     * Determines whether the given User is the assigned faculty for the given
     * ActivityAssignment.
     * Checks temporary assignments for today first, then falls back to permanent
     * assignment.
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

        // 2. Check Temporary Assignment Priority for Today
        if (assignment.getActivity() != null) {
            Long deptId = assignment.getDepartment() != null ? assignment.getDepartment().getId() : null;
            Long secId = assignment.getSection() != null ? assignment.getSection().getId() : null;
            LocalDate today = LocalDate.now();

            List<ActivityTemporaryAssignment> tempAssignments = temporaryAssignmentRepository.findActiveAssignments(
                    assignment.getActivity().getId(),
                    deptId,
                    secId,
                    today);

            if (!tempAssignments.isEmpty()) {
                ActivityTemporaryAssignment activeTemp = tempAssignments.get(0);
                if (activeTemp.getTemporaryTeacher() != null
                        && activeTemp.getTemporaryTeacher().getId().equals(user.getId())) {
                    return true;
                }
                // If user is the original/permanent teacher replaced for today, they do not
                // have active assignment today
                if (activeTemp.getOriginalTeacher() != null
                        && activeTemp.getOriginalTeacher().getId().equals(user.getId())) {
                    return false;
                }
                if (assignment.getTeacher() != null && assignment.getTeacher().getId().equals(user.getId())) {
                    return false;
                }
            }
        }

        // 3. Global Scope (Only the assigned Teacher can perform/award)
        if (assignment.getAssignmentScope() == AssignmentScope.GLOBAL) {
            return assignment.getTeacher() != null && assignment.getTeacher().getId().equals(user.getId());
        }

        // 4. Specific Faculty Scope
        if (assignment.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) {
            return assignment.getTeacher() != null && assignment.getTeacher().getId().equals(user.getId());
        }

        // 5. Department / Section Scope
        if (assignment.getAssignmentScope() == AssignmentScope.DEPARTMENT
                || assignment.getAssignmentScope() == AssignmentScope.SECTION) {

            // 5a. If a specific teacher is directly assigned on this record, authorise that
            // teacher.
            if (assignment.getTeacher() != null && assignment.getTeacher().getId().equals(user.getId())) {
                return true;
            }

            // 5b. Otherwise fall back to Class-Coordinator resolution:
            if (assignment.getDepartment() != null && assignment.getSection() != null) {
                List<User> classCoordinators = userRepository.findClassCoordinatorsByDepartmentAndSection(
                        assignment.getDepartment().getId(),
                        assignment.getSection().getId());

                return classCoordinators.stream().anyMatch(cc -> cc.getId().equals(user.getId()));
            }
        }

        return false;
    }
}
