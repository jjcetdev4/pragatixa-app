package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.Role;
import jjcet.PragatiX.entity.SubRole;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.activity.service.AssignmentSecurityService;
import org.springframework.stereotype.Component;

@Component
public class TeamValidationService {

    private final AssignmentSecurityService assignmentSecurityService;
    private final jjcet.PragatiX.modules.authentication.security.AuthUtils authUtils;

    public TeamValidationService(AssignmentSecurityService assignmentSecurityService,
            jjcet.PragatiX.modules.authentication.security.AuthUtils authUtils) {
        this.assignmentSecurityService = assignmentSecurityService;
        this.authUtils = authUtils;
    }

    public boolean canCreateTeam(User creator, ActivityAssignment assignment) {
        boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = creator.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR"));
        boolean isHod = creator.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));
        boolean isAssignedFaculty = false;

        if (assignment != null && assignment.getTeacher() != null) {
            isAssignedFaculty = assignment.getTeacher().getUsername().equals(creator.getUsername());
        }

        return isAdmin || isCc || isHod || isAssignedFaculty;
    }

    public boolean canDeleteTeam(User currentUser, ActivityAssignment assignment) {
        if (assignment == null)
            return false;
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = currentUser.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR"));
        boolean isHod = currentUser.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));
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

        boolean matchesHodDept = false;
        if (isHod && assignment.getDepartment() != null && currentUser.getDepartment() != null) {
            if (assignment.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                matchesHodDept = true;
            }
        }

        return isAdmin || isAssignedFaculty || matchesDeptAndSection || matchesHodDept;
    }

    public boolean validateTeamAccess(User user, jjcet.PragatiX.entity.Team team) {
        if (authUtils.isSuperAdmin(user))
            return true;

        jjcet.PragatiX.entity.Department teamDept = team.getDepartment() != null ? team.getDepartment()
                : (team.getCaptain() != null ? team.getCaptain().getDepartment() : null);

        if (team.getCreatedBy() != null && team.getCreatedBy().getId().equals(user.getId())) {
            boolean isCcOrHod = user.getRoles().stream().map(Role::getName)
                    .anyMatch(r -> r != null && (r.trim().equalsIgnoreCase("CC") || r.trim().equalsIgnoreCase("CLASS_COORDINATOR") || r.trim().equalsIgnoreCase("HOD") || r.trim().equalsIgnoreCase("ROLE_HOD"))) ||
                    user.getSubRoles().stream().map(SubRole::getName)
                    .anyMatch(sr -> sr != null && (sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR") || sr.trim().equalsIgnoreCase("HOD") || sr.trim().equalsIgnoreCase("ROLE_HOD")));
            if (isCcOrHod) {
                if (user.getDepartment() == null || teamDept == null || teamDept.getId().equals(user.getDepartment().getId())) {
                    return true;
                }
            } else {
                return true;
            }
        }

        if (authUtils.isAdmin(user)) {
            String adminYear = jjcet.PragatiX.modules.authentication.security.AuthUtils
                    .getAssignedYearString(user.getAcademicYear());
            if (adminYear != null && adminYear.equals(team.getYear())) {
                return true;
            }
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to view this team's details.");
        }

        jjcet.PragatiX.entity.Section teamSec = team.getSection() != null ? team.getSection()
                : (team.getCaptain() != null ? team.getCaptain().getSection() : null);

        boolean isCc = user.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR"));
        if (isCc) {
            boolean matchesDept = teamDept != null && user.getDepartment() != null
                    && teamDept.getId().equals(user.getDepartment().getId());
                    
            boolean matchesSection = false;
            if (user.getSection() == null) {
                // CC handles the whole department or no specific section is set
                matchesSection = true;
            } else if (teamSec == null) {
                // Team has no section, but CC has a section. We can allow if dept matches.
                matchesSection = true;
            } else {
                matchesSection = teamSec.getId().equals(user.getSection().getId());
            }

            if (matchesDept && matchesSection) {
                return true;
            }
        }

        boolean isHod = user.getRoles().stream().map(Role::getName)
                .anyMatch(r -> r != null && (r.trim().equalsIgnoreCase("HOD") || r.trim().equalsIgnoreCase("ROLE_HOD"))) ||
                user.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr != null && (sr.trim().equalsIgnoreCase("HOD") || sr.trim().equalsIgnoreCase("ROLE_HOD")));
        if (isHod) {
            boolean matchesDept = teamDept != null && user.getDepartment() != null
                    && teamDept.getId().equals(user.getDepartment().getId());
            if (matchesDept) {
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
