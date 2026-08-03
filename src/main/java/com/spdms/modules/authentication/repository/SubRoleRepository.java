package com.spdms.modules.authentication.repository;

import com.spdms.entity.SubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubRoleRepository extends JpaRepository<SubRole, Long> {
    Optional<SubRole> findByName(String name);
}
