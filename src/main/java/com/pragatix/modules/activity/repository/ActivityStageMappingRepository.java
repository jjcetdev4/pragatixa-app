package com.pragatix.modules.activity.repository;

import com.pragatix.entity.ActivityStageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStageMappingRepository extends JpaRepository<ActivityStageMapping, Long> {
    boolean existsByStageIdAndActivityId(Long stageId, Long activityId);

    void deleteByActivityId(Long activityId);

    List<ActivityStageMapping> findByStageId(Long stageId);

    List<ActivityStageMapping> findByActivityId(Long activityId);

    List<ActivityStageMapping> findBySubgroupId(Long subgroupId);

    Optional<ActivityStageMapping> findByStageIdAndActivityId(Long stageId, Long activityId);
}
