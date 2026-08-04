package com.pragatix.repository;

import com.pragatix.entity.StageTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageTeamRepository extends JpaRepository<StageTeam, Long> {
    Optional<StageTeam> findByStageIdAndTeamId(Long stageId, Long teamId);

    List<StageTeam> findByStageId(Long stageId);

    List<StageTeam> findByTeamId(Long teamId);
}
