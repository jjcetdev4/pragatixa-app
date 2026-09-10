package jjcet.PragatiX.modules.authentication.security;

import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Loads User (teacher/admin) details from the database for Spring Security.
 */
@Service
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> (u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(username))
                                || (u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(username)))
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found with username or email: " + username)));

        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();

        if (user.getRoles() != null) {
            for (jjcet.PragatiX.entity.Role role : user.getRoles()) {
                if (role.getName() == null)
                    continue;
                String rName = role.getName().trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority(rName));
                if (!rName.startsWith("ROLE_")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + rName));
                }

                if (rName.equals("SUPERADMIN") || rName.equals("SUPER_ADMIN") ||
                        rName.equals("ROLE_SUPERADMIN") || rName.equals("ROLE_SUPER_ADMIN")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("SUPERADMIN"));
                    authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
                    // Automatically grant ADMIN rights to SUPER_ADMIN
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                }
                if (rName.equals("FACULTY") || rName.equals("TEACHER") ||
                        rName.equals("ROLE_FACULTY") || rName.equals("ROLE_TEACHER")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_FACULTY"));
                    authorities.add(new SimpleGrantedAuthority("TEACHER"));
                    authorities.add(new SimpleGrantedAuthority("FACULTY"));
                }
                if (rName.equals("HOD") || rName.equals("ROLE_HOD")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_HOD"));
                    authorities.add(new SimpleGrantedAuthority("HOD"));

                }
                if (rName.equals("ADMIN") || rName.equals("ROLE_ADMIN")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                }
            }
        }

        if (user.getSubRoles() != null) {
            for (jjcet.PragatiX.entity.SubRole sr : user.getSubRoles()) {
                if (sr.getName() == null)
                    continue;
                String srName = sr.getName().trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority(srName));
                if (!srName.startsWith("ROLE_")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + srName));
                }
                if (srName.equals("CC") || srName.equals("ROLE_CC")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_CLASS_COORDINATOR"));
                    authorities.add(new SimpleGrantedAuthority("CLASS_COORDINATOR"));
                }
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password("")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}
// newly added something