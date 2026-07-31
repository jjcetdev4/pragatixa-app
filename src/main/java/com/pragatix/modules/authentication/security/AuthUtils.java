package com.pragatix.modules.authentication.security;

import com.pragatix.entity.Role;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    private final UserRepository userRepository;

    public AuthUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets the currently authenticated user based on the SecurityContextHolder.
     * 
     * @return User object or null if not authenticated/found.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Maps the User's assigned AcademicYear enum to the String representation
     * used in the Student and Team entities ("1", "2", "3", "4").
     * 
     * @param year the AcademicYear enum
     * @return the string mapped value
     */
    public static String getAssignedYearString(com.pragatix.enums.AcademicYear year) {
        if (year == null)
            return null;
        switch (year) {
            case FIRST_YEAR:
                return "1";
            case SECOND_YEAR:
                return "2";
            case THIRD_YEAR:
                return "3";
            case FOURTH_YEAR:
                return "4";
            default:
                return null;
        }
    }

    public boolean isSuperAdmin(User user) {
        if (user == null || user.getRoles() == null)
            return false;
        for (Role role : user.getRoles()) {
            if ("ROLE_SUPERADMIN".equalsIgnoreCase(role.getName()) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null)
            return false;
        for (Role role : user.getRoles()) {
            if ("ROLE_ADMIN".equalsIgnoreCase(role.getName())) {
                return true;
            }
        }
        return false;
    }
}
