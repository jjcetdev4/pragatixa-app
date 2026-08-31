package jjcet.PragatiX.modules.analytics.scoped.service;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Faculty;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.analytics.util.AnalyticsRoleUtils;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.faculty.repository.FacultyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Default implementation of {@link AnalyticsScopeResolver}.
 *
 * <p>Resolves scope from the authenticated User's entity associations:</p>
 * <ul>
 *   <li>Super Admin / Admin → INSTITUTION (no department restriction)</li>
 *   <li>HOD → DEPARTMENT (User.department)</li>
 *   <li>Faculty/Teacher → SECTION (Faculty.department + Faculty.section)</li>
 *   <li>Student → SELF (no department scope)</li>
 * </ul>
 */
// Legacy implementation superseded by domain.scope.AnalyticsScopeResolverImpl
// @Service
public class AnalyticsScopeResolverImpl implements AnalyticsScopeResolver {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsScopeResolverImpl.class);

    private final AuthUtils authUtils;
    private final FacultyRepository facultyRepository;

    public AnalyticsScopeResolverImpl(AuthUtils authUtils,
                                       FacultyRepository facultyRepository) {
        this.authUtils = authUtils;
        this.facultyRepository = facultyRepository;
    }

    @Override
    public ScopeLevel resolveScopeLevel(User user) {
        if (user == null) {
            return null;
        }
        if (authUtils.isSuperAdmin(user)) {
            return ScopeLevel.INSTITUTION;
        }
        if (authUtils.isAdmin(user)) {
            return ScopeLevel.INSTITUTION;
        }
        if (AnalyticsRoleUtils.isHod(user)) {
            return ScopeLevel.DEPARTMENT;
        }
        if (AnalyticsRoleUtils.isFaculty(user)) {
            return ScopeLevel.SECTION;
        }
        return ScopeLevel.SELF;
    }

    @Override
    public Long getPermittedDepartmentId(User user) {
        if (user == null) {
            return null;
        }
        if (authUtils.isSuperAdmin(user) || authUtils.isAdmin(user)) {
            return null;
        }
        if (AnalyticsRoleUtils.isHod(user)) {
            Department dept = user.getDepartment();
            return dept != null ? dept.getId() : null;
        }
        if (AnalyticsRoleUtils.isFaculty(user)) {
            Faculty faculty = facultyRepository.findByUserUsername(user.getUsername()).orElse(null);
            if (faculty != null && faculty.getDepartment() != null) {
                return faculty.getDepartment().getId();
            }
            return null;
        }
        return null;
    }

    @Override
    public Long getPermittedSectionId(User user) {
        if (user == null) {
            return null;
        }
        if (AnalyticsRoleUtils.isFaculty(user)) {
            Faculty faculty = facultyRepository.findByUserUsername(user.getUsername()).orElse(null);
            if (faculty != null) {
                Section section = faculty.getSection();
                return section != null ? section.getId() : null;
            }
        }
        return null;
    }

    @Override
    public void validateDepartmentAccess(User user, Long requestedDepartmentId) {
        if (user == null) {
            log.warn("validateDepartmentAccess: user is null, denying access");
            throw new AccessDeniedException("User is not authenticated");
        }

        if (requestedDepartmentId == null) {
            return;
        }

        if (requestedDepartmentId <= 0) {
            throw new AccessDeniedException("Department ID must be a positive value");
        }

        ScopeLevel scope = resolveScopeLevel(user);
        if (scope == ScopeLevel.INSTITUTION) {
            return;
        }

        Long permittedDeptId = getPermittedDepartmentId(user);
        if (permittedDeptId == null) {
            throw new AccessDeniedException(
                    "User has no department scope; cannot access department analytics"
            );
        }

        if (!Objects.equals(permittedDeptId, requestedDepartmentId)) {
            log.warn("Department access denied: user permitted dept={}, requested dept={}",
                    permittedDeptId, requestedDepartmentId);
            throw new AccessDeniedException(
                    "Access denied: department " + requestedDepartmentId + " is outside your scope"
            );
        }
    }

    @Override
    public void validateSectionAccess(User user, Long requestedSectionId) {
        if (user == null) {
            log.warn("validateSectionAccess: user is null, denying access");
            throw new AccessDeniedException("User is not authenticated");
        }

        if (requestedSectionId == null) {
            return;
        }

        if (requestedSectionId <= 0) {
            throw new AccessDeniedException("Section ID must be a positive value");
        }

        ScopeLevel scope = resolveScopeLevel(user);
        if (scope == ScopeLevel.INSTITUTION || scope == ScopeLevel.DEPARTMENT) {
            return;
        }

        Long permittedSectionId = getPermittedSectionId(user);
        if (permittedSectionId == null) {
            throw new AccessDeniedException(
                    "User has no section scope; cannot access section analytics"
            );
        }

        if (!Objects.equals(permittedSectionId, requestedSectionId)) {
            log.warn("Section access denied: user permitted section={}, requested section={}",
                    permittedSectionId, requestedSectionId);
            throw new AccessDeniedException(
                    "Access denied: section " + requestedSectionId + " is outside your scope"
            );
        }
    }
}
