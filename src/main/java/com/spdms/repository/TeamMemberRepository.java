package com.spdms.repository;

import com.spdms.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    long countByTeamId(Long teamId);
    boolean existsByTeamId(Long teamId);
}
