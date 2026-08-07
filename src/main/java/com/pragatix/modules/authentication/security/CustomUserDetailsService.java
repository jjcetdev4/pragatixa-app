package com.pragatix.modules.authentication.security;

import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
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
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        if (user.getSubRoles() != null) {
            for (com.pragatix.entity.SubRole sr : user.getSubRoles()) {
                String srName = sr.getName().toUpperCase();
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
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}
