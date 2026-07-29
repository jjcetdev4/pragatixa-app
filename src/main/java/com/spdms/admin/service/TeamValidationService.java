package com.pragatix.admin.service;

import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.SubRole;
import com.pragatix.entity.User;
import com.pragatix.modules.activity.service.AssignmentSecurityService;
import org.springframework.stereotype.Component;

@Component
public class TeamValidationService {

    private final AssignmentSecurityService assignmentSecurityService;
    private final com.pragatix.modules.authentication.security.AuthUtils authUtils;

    public TeamValidationService(AssignmentSecurityService assignmentSecurityService,
            com.pragatix.modules.authentication.security.AuthUtils authUtils) {
        this.assignmentSecurityService = assignmentSecurityService;
        this.authUtils = authUtils;
    }

    public boolean canCreateTeam(User creator, ActivityAssignment assignment) {
        boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = creator.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        boolean isAssignedFaculty = false;

        if (assignment != null && assignment.getTeacher() != null) {
            isAssignedFaculty = assignment.getTeacher().getUsername().equals(creator.getUsername());
        }

        return isAdmin || isCc || isAssignedFaculty;
    }

    public boolean canDeleteTeam(User currentUser, ActivityAssignment assignment) {
        if (assignment == null)
            return false;
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = currentUser.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        boolean isAssignedFaculty = assignment.getTeacher() != null
                && assignment.getTeacher().getUsername().equals(currentUser.getUsername());

        boolean matchesDeptAndSection = false;
        if (isCc && assignment.getDepartment() != null && currentUser.getDepartment() != null) {
            if (assignment.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                if (assignment.getSection() == null || (currentUser.getSection() != null
                        && assignment.getSection().getId().equals(currentUser.getSection().getId()))) {
                    matchesDeptAndSection = true;
                }
            }
        }

        return isAdmin || isAssignedFaculty || matchesDeptAndSection;
    }

    public boolean validateTeamAccess(User user, com.pragatix.entity.Team team) {
        if (authUtils.isSuperAdmin(user))
            return true;

        if (authUtils.isAdmin(user)) {
            String adminYear = com.pragatix.modules.authentication.security.AuthUtils
                    .getAssignedYearString(user.getAcademicYear());
            if (adminYear != null && adminYear.equals(team.getYear())) {
                return true;
            }
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to view this team's details.");
        }

        boolean isCc = user.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        if (isCc) {
            boolean matchesDept = team.getDepartment() != null && user.getDepartment() != null
                    && team.getDepartment().getId().equals(user.getDepartment().getId());
            boolean matchesSection = team.getSection() != null && user.getSection() != null
                    && team.getSection().getId().equals(user.getSection().getId());

            if (matchesDept && matchesSection) {
                return true;
            }
        }

        boolean isStudent = user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_STUDENT"));
        if (isStudent) {
            boolean isCaptain = team.getCaptain() != null
                    && team.getCaptain().getRegNo().equalsIgnoreCase(user.getUsername());
            boolean isMember = team.getMembers() != null && team.getMembers().stream()
                    .anyMatch(member -> member.getRegNo().equalsIgnoreCase(user.getUsername()));
            if (isCaptain || isMember) {
                return true;
            } else {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to view this team's details.");
            }
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "You do not have permission to manage this team.");
    }
}
