package jjcet.PragatiX.modules.analytics.scoped.service;

import jjcet.PragatiX.entity.User;

/**
 * Resolves the permitted organizational scope for an authenticated user
 * requesting Analytics data.
 *
 * <p>Scope levels (most to least restrictive):</p>
 * <ul>
 *   <li>INSTITUTION — Super Admin and Admin see all data.</li>
 *   <li>DEPARTMENT — HOD is restricted to the assigned department.</li>
 *   <li>SECTION — Faculty (Class Coordinator / Teacher) is restricted to the
 *       assigned department and section.</li>
 *   <li>SELF — Student is restricted to self, with no department scope.</li>
 * </ul>
 */
public interface AnalyticsScopeResolver {

    /**
     * The permitted organizational scope for the given user.
     */
    enum ScopeLevel {
        INSTITUTION, DEPARTMENT, SECTION, SELF
    }

    /**
     * Resolves the permitted {@link ScopeLevel} for the given user.
     *
     * @param user the authenticated user, may be {@code null}
     * @return the scope level, or {@code null} if the user has no analytics scope
     */
    ScopeLevel resolveScopeLevel(User user);

    /**
     * Returns the department ID the user is permitted to see, or {@code null}
     * if the user has institution-wide scope.
     *
     * @param user the authenticated user
     * @return permitted department id, or {@code null} for institution scope
     */
    Long getPermittedDepartmentId(User user);

    /**
     * Returns the section ID the user is permitted to see, or {@code null}
     * if the user has department-wide (or wider) scope.
     *
     * @param user the authenticated user
     * @return permitted section id, or {@code null} if no section restriction
     */
    Long getPermittedSectionId(User user);

    /**
     * Validates that the user is permitted to view analytics for the given
     * department ID. Throws {@link org.springframework.security.access.AccessDeniedException}
     * if the department is outside the user's permitted scope.
     *
     * @param user        the authenticated user
     * @param departmentId the department ID requested by the client
     * @throws org.springframework.security.access.AccessDeniedException if access is denied
     */
    void validateDepartmentAccess(User user, Long departmentId);

    /**
     * Validates that the user is permitted to view analytics for the given
     * section ID. Throws {@link org.springframework.security.access.AccessDeniedException}
     * if the section is outside the user's permitted scope.
     *
     * @param user       the authenticated user
     * @param sectionId  the section ID requested by the client
     * @throws org.springframework.security.access.AccessDeniedException if access is denied
     */
    void validateSectionAccess(User user, Long sectionId);
}
