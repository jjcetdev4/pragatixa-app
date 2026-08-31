package jjcet.PragatiX.modules.analytics.util;

import jjcet.PragatiX.entity.Role;
import jjcet.PragatiX.entity.SubRole;
import jjcet.PragatiX.entity.User;

import java.util.Objects;

public final class AnalyticsRoleUtils {

    private AnalyticsRoleUtils() {
    }

    public static boolean isSuperAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> {
                    String trimmed = name.trim().toUpperCase();
                    return "ROLE_SUPERADMIN".equals(trimmed) || "ROLE_SUPER_ADMIN".equals(trimmed)
                            || "SUPERADMIN".equals(trimmed) || "SUPER_ADMIN".equals(trimmed);
                });
    }

    public static boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> {
                    String trimmed = name.trim().toUpperCase();
                    return "ROLE_ADMIN".equals(trimmed) || "ADMIN".equals(trimmed);
                });
    }

    public static boolean isHod(User user) {
        if (user == null) {
            return false;
        }
        if (user.getSubRoles() != null) {
            boolean matchSubRole = user.getSubRoles().stream()
                    .filter(Objects::nonNull)
                    .map(SubRole::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> {
                        String trimmed = name.trim().toUpperCase();
                        return "HOD".equals(trimmed) || "ROLE_HOD".equals(trimmed);
                    });
            if (matchSubRole) {
                return true;
            }
        }
        if (user.getRoles() != null) {
            return user.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> {
                        String trimmed = name.trim().toUpperCase();
                        return "HOD".equals(trimmed) || "ROLE_HOD".equals(trimmed);
                    });
        }
        return false;
    }

    public static boolean isFaculty(User user) {
        if (user == null) {
            return false;
        }
        if (user.getRoles() != null) {
            boolean matchRole = user.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> {
                        String trimmed = name.trim().toUpperCase();
                        return "FACULTY".equals(trimmed) || "ROLE_FACULTY".equals(trimmed)
                                || "TEACHER".equals(trimmed) || "ROLE_TEACHER".equals(trimmed);
                    });
            if (matchRole) {
                return true;
            }
        }
        if (user.getSubRoles() != null) {
            return user.getSubRoles().stream()
                    .filter(Objects::nonNull)
                    .map(SubRole::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> {
                        String trimmed = name.trim().toUpperCase();
                        return "FACULTY".equals(trimmed) || "ROLE_FACULTY".equals(trimmed)
                                || "TEACHER".equals(trimmed) || "ROLE_TEACHER".equals(trimmed);
                    });
        }
        return false;
    }

    public static boolean isStudent(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> {
                    String trimmed = name.trim().toUpperCase();
                    return "STUDENT".equals(trimmed) || "ROLE_STUDENT".equals(trimmed);
                });
    }
}
