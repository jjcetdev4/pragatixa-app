package jjcet.PragatiX.modules.analytics.domain.model;

public record Scope(
        ScopeLevel level,
        Long departmentId,
        Long sectionId
) {
    public enum ScopeLevel {
        INSTITUTION,
        DEPARTMENT,
        SECTION,
        SELF
    }

    public static Scope institution() {
        return new Scope(ScopeLevel.INSTITUTION, null, null);
    }

    public static Scope department(Long departmentId) {
        return new Scope(ScopeLevel.DEPARTMENT, departmentId, null);
    }

    public static Scope section(Long departmentId, Long sectionId) {
        return new Scope(ScopeLevel.SECTION, departmentId, sectionId);
    }

    public static Scope self() {
        return new Scope(ScopeLevel.SELF, null, null);
    }
}