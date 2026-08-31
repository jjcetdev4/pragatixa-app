package jjcet.PragatiX.modules.analytics.domain.scope;

import jjcet.PragatiX.modules.analytics.domain.model.Scope;
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
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AnalyticsScopeResolverImpl implements ScopeResolver {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsScopeResolverImpl.class);

    private final AuthUtils authUtils;
    private final FacultyRepository facultyRepository;

    public AnalyticsScopeResolverImpl(AuthUtils authUtils, FacultyRepository facultyRepository) {
        this.authUtils = authUtils;
        this.facultyRepository = facultyRepository;
    }

    @Override
    public Scope resolveScope(User user) {
        if (user == null) {
            return Scope.self();
        }
        if (authUtils.isSuperAdmin(user) || authUtils.isAdmin(user)) {
            return Scope.institution();
        }
        if (AnalyticsRoleUtils.isHod(user)) {
            Department dept = user.getDepartment();
            return dept != null ? Scope.department(dept.getId()) : Scope.self();
        }
        if (AnalyticsRoleUtils.isFaculty(user)) {
            Faculty faculty = facultyRepository.findByUserUsername(user.getUsername()).orElse(null);
            if (faculty != null) {
                Long deptId = faculty.getDepartment() != null ? faculty.getDepartment().getId() : null;
                Long sectionId = faculty.getSection() != null ? faculty.getSection().getId() : null;
                return Scope.section(deptId, sectionId);
            }
            return Scope.self();
        }
        return Scope.self();
    }

    @Override
    public Long getDepartmentId(User user) {
        Scope scope = resolveScope(user);
        return scope.departmentId();
    }

    @Override
    public Long getSectionId(User user) {
        Scope scope = resolveScope(user);
        return scope.sectionId();
    }

    @Override
    public void validateDepartmentAccess(User user, Long departmentId) {
        if (user == null) {
            log.warn("validateDepartmentAccess: user is null, denying access");
            throw new AccessDeniedException("User is not authenticated");
        }
        if (departmentId == null) return;
        if (departmentId <= 0) {
            throw new AccessDeniedException("Department ID must be a positive value");
        }
        Scope scope = resolveScope(user);
        if (scope.level() == Scope.ScopeLevel.INSTITUTION) return;

        Long permitted = scope.departmentId();
        if (permitted == null) {
            throw new AccessDeniedException("User has no department scope; cannot access department analytics");
        }
        if (!Objects.equals(permitted, departmentId)) {
            log.warn("Department access denied: user permitted dept={}, requested dept={}", permitted, departmentId);
            throw new AccessDeniedException("Access denied: department " + departmentId + " is outside your scope");
        }
    }

    @Override
    public void validateSectionAccess(User user, Long sectionId) {
        if (user == null) {
            log.warn("validateSectionAccess: user is null, denying access");
            throw new AccessDeniedException("User is not authenticated");
        }
        if (sectionId == null) return;
        if (sectionId <= 0) {
            throw new AccessDeniedException("Section ID must be a positive value");
        }
        Scope scope = resolveScope(user);
        if (scope.level() == Scope.ScopeLevel.INSTITUTION || scope.level() == Scope.ScopeLevel.DEPARTMENT) {
            return;
        }
        Long permitted = scope.sectionId();
        if (permitted == null) {
            throw new AccessDeniedException("User has no section scope; cannot access section analytics");
        }
        if (!Objects.equals(permitted, sectionId)) {
            log.warn("Section access denied: user permitted section={}, requested section={}", permitted, sectionId);
            throw new AccessDeniedException("Access denied: section " + sectionId + " is outside your scope");
        }
    }
}