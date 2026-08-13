package jjcet.PragatiX.modules.authentication.security;

import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import jjcet.PragatiX.repository.StageTeamRepository;
import org.springframework.security.core.GrantedAuthority;

/**
 * Dedicated UserDetailsService for Student authentication.
 * Searches ONLY the students table.
 */
@Service
public class StudentDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(StudentDetailsService.class);
    private final StudentRepository studentRepository;
    private final StageTeamRepository stageTeamRepository;

    public StudentDetailsService(StudentRepository studentRepository, StageTeamRepository stageTeamRepository) {
        this.studentRepository = studentRepository;
        this.stageTeamRepository = stageTeamRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[StudentDetailsService] Loading student details for identifier: {}", username);

        java.util.Optional<Student> studentOpt = studentRepository.findByRegNo(username)
                .or(() -> studentRepository.findByEmail(username))
                .or(() -> studentRepository.findBySprNo(username));

        Student student = studentOpt.orElseThrow(() -> {
            log.warn("[StudentDetailsService] Student not found with identifier: {}", username);
            return new UsernameNotFoundException("Student not found with identifier: " + username);
        });

        log.debug("[StudentDetailsService] Found student: reg_no={}, active={}", student.getRegNo(),
                student.isActive());

        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());
        boolean isViceCap = false;

        if (student.getTeam() != null) {
            if (student.getTeam().getViceCaptain() != null
                    && student.getTeam().getViceCaptain().getId().equals(student.getId())) {
                isViceCap = true;
            }

            // Check StageTeams for captaincy/vice-captaincy if not already identified
            if (!isCap || !isViceCap) {
                List<jjcet.PragatiX.entity.StageTeam> stageTeams = stageTeamRepository
                        .findByTeamId(student.getTeam().getId());
                for (jjcet.PragatiX.entity.StageTeam st : stageTeams) {
                    if (!isCap && st.getCaptain() != null && st.getCaptain().getId().equals(student.getId())) {
                        isCap = true;
                    }
                    if (!isViceCap && st.getViceCaptain() != null
                            && st.getViceCaptain().getId().equals(student.getId())) {
                        isViceCap = true;
                    }
                    if (isCap && isViceCap)
                        break;
                }
            }
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        if (isCap) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CAPTAIN"));
            authorities.add(new SimpleGrantedAuthority("CAPTAIN"));
        }
        if (isViceCap) {
            authorities.add(new SimpleGrantedAuthority("ROLE_VICE_CAPTAIN"));
            authorities.add(new SimpleGrantedAuthority("VICE_CAPTAIN"));
        }

        return User.builder()
                .username(student.getRegNo())
                .password(student.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!student.isActive())
                .build();
    }
}
