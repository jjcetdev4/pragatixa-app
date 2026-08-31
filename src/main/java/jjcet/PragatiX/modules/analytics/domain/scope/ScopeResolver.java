package jjcet.PragatiX.modules.analytics.domain.scope;

import jjcet.PragatiX.modules.analytics.domain.model.Scope;
import jjcet.PragatiX.entity.User;

public interface ScopeResolver {
    Scope resolveScope(User user);

    Long getDepartmentId(User user);

    Long getSectionId(User user);

    void validateDepartmentAccess(User user, Long departmentId);

    void validateSectionAccess(User user, Long sectionId);
}