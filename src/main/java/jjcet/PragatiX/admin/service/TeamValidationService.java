package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.Role;
import jjcet.PragatiX.entity.Student;
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
        if (creator == null) return false;
        if (authUtils.isSuperAdmin(creator) || authUtils.isAdmin(creator)) {
            return true;
        }
        boolean isAdmin = creator.getRoles().stream().anyMatch(r -> {
            String name = r.getName() != null ? r.getName().toUpperCase() : "";
            return name.contains("ADMIN");
        });
        boolean isCc = creator.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr != null && (sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR")));
        boolean isHod = creator.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr != null && sr.trim().equalsIgnoreCase("HOD")) ||
                creator.getRoles().stream().map(Role::getName)
                .anyMatch(r -> r != null && (r.trim().equalsIgnoreCase("HOD") || r.trim().equalsIgnoreCase("ROLE_HOD")));
        boolean isAssignedFaculty = false;

        if (assignment != null && assignment.getTeacher() != null) {
            isAssignedFaculty = assignment.getTeacher().getUsername().equals(creator.getUsername());
        }

        return isAdmin || isCc || isHod || isAssignedFaculty;
    }

    public boolean canDeleteTeam(User currentUser, ActivityAssignment assignment) {
        if (currentUser == null) return false;
        if (authUtils.isSuperAdmin(currentUser) || authUtils.isAdmin(currentUser)) {
            return true;
        }
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> {
            String name = r.getName() != null ? r.getName().toUpperCase() : "";
            return name.contains("ADMIN");
        });
        if (assignment == null) {
            return isAdmin;
        }
        boolean isCc = currentUser.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr != null && (sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR")));
        boolean isHod = currentUser.getSubRoles().stream().map(SubRole::getName)
                .anyMatch(sr -> sr != null && sr.trim().equalsIgnoreCase("HOD")) ||
                currentUser.getRoles().stream().map(Role::getName)
                .anyMatch(r -> r != null && (r.trim().equalsIgnoreCase("HOD") || r.trim().equalsIgnoreCase("ROLE_HOD")));
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
            if (adminYear == null) {
                return true;
            }
            if (isMatchingYear(adminYear, team.getYear())) {
                return true;
            }
            if (team.getCaptain() != null) {
                if (team.getCaptain().getYearRef() != null &&
                        isMatchingYear(adminYear, String.valueOf(team.getCaptain().getYearRef().getYearNo()))) {
                    return true;
                }
                if (isMatchingYear(adminYear, team.getCaptain().getYear())) {
                    return true;
                }
            }
            if (team.getMembers() != null && !team.getMembers().isEmpty()) {
                for (Student m : team.getMembers()) {
                    if (m.getYearRef() != null && isMatchingYear(adminYear, String.valueOf(m.getYearRef().getYearNo()))) {
                        return true;
                    }
                    if (isMatchingYear(adminYear, m.getYear())) {
                        return true;
                    }
                }
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

    public static boolean isMatchingYear(String yearA, String yearB) {
        if (yearA == null || yearB == null) return false;
        String a = normalizeYear(yearA);
        String b = normalizeYear(yearB);
        return a != null && a.equals(b);
    }

    public static String normalizeYear(String year) {
        if (year == null) return null;
        String clean = year.trim().toUpperCase();
        if (clean.equals("1") || clean.contains("FIRST") || clean.contains("1ST") || clean.equals("I")) return "1";
        if (clean.equals("2") || clean.contains("SECOND") || clean.contains("2ND") || clean.equals("II")) return "2";
        if (clean.equals("3") || clean.contains("THIRD") || clean.contains("3RD") || clean.equals("III")) return "3";
        if (clean.equals("4") || clean.contains("FOURTH") || clean.contains("4TH") || clean.equals("IV")) return "4";
        return clean;
    }

    public static String validateStudentClassMatch(Student student, String roleLabel, Long deptId, String canonicalYear, Long sectionId) {
        if (student == null) {
            return roleLabel + " not found.";
        }

        // 1. Department match
        if (deptId != null) {
            if (student.getDepartment() == null || !deptId.equals(student.getDepartment().getId())) {
                String studentDept = student.getDepartment() != null ? student.getDepartment().getName() : "None";
                return roleLabel + " " + student.getFullName() + " belongs to a different department (" + studentDept + ") than the team.";
            }
        }

        // 2. Year match
        if (canonicalYear != null && !canonicalYear.trim().isEmpty()) {
            String teamCanonicalYear = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(canonicalYear);
            String studentCanonicalYear = null;
            if (student.getYear() != null && !student.getYear().trim().isEmpty()) {
                studentCanonicalYear = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYear());
            } else if (student.getYearRef() != null) {
                if (student.getYearRef().getYearNo() != null) {
                    studentCanonicalYear = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(String.valueOf(student.getYearRef().getYearNo()));
                } else if (student.getYearRef().getYearName() != null) {
                    studentCanonicalYear = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYearRef().getYearName());
                }
            }
            if (teamCanonicalYear != null && (studentCanonicalYear == null || !studentCanonicalYear.equalsIgnoreCase(teamCanonicalYear.trim()))) {
                return roleLabel + " " + student.getFullName() + " is in a different academic year than the team.";
            }
        }

        // 3. Section match
        if (sectionId != null) {
            if (student.getSection() == null || !sectionId.equals(student.getSection().getId())) {
                String studentSec = student.getSection() != null ? student.getSection().getSectionName() : "None";
                return roleLabel + " " + student.getFullName() + " belongs to a different section (" + studentSec + ") than the team.";
            }
        }

        return null;
    }
}
