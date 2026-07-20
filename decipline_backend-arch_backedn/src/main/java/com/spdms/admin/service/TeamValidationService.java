package com.spdms.admin.service;

import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.SubRole;
import com.spdms.entity.User;
import com.spdms.modules.activity.service.AssignmentSecurityService;
import org.springframework.stereotype.Component;

@Component
public class TeamValidationService {

    private final AssignmentSecurityService assignmentSecurityService;

    public TeamValidationService(AssignmentSecurityService assignmentSecurityService) {
        this.assignmentSecurityService = assignmentSecurityService;
    }

    public boolean canCreateTeam(User creator, ActivityAssignment assignment) {
        boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        boolean isAssignedFaculty = false;

        if (assignment != null && assignment.getTeacher() != null) {
            isAssignedFaculty = assignment.getTeacher().getUsername().equals(creator.getUsername());
        }

        return isAdmin || isCc || isAssignedFaculty;
    }

    public boolean canDeleteTeam(User currentUser, ActivityAssignment assignment) {
        if (assignment == null) return false;
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = currentUser.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        boolean isAssignedFaculty = assignment.getTeacher() != null && assignment.getTeacher().getUsername().equals(currentUser.getUsername());
        
        boolean matchesDeptAndSection = false;
        if (isCc && assignment.getDepartment() != null && currentUser.getDepartment() != null) {
            if (assignment.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                if (assignment.getSection() == null || (currentUser.getSection() != null && assignment.getSection().getId().equals(currentUser.getSection().getId()))) {
                    matchesDeptAndSection = true;
                }
            }
        }
        
        return isAdmin || isAssignedFaculty || matchesDeptAndSection;
    }
}
