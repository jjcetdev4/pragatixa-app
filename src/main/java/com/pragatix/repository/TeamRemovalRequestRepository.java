package com.pragatix.repository;

import com.pragatix.entity.TeamRemovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamRemovalRequestRepository extends JpaRepository<TeamRemovalRequest, Long> {
    List<TeamRemovalRequest> findByStatus(String status);

    List<TeamRemovalRequest> findByTeamIdAndStatus(Long teamId, String status);

    boolean existsByTeamIdAndStudentRegNoAndStatus(Long teamId, String regNo, String status);

    List<TeamRemovalRequest> findByTeamId(Long teamId);
}
