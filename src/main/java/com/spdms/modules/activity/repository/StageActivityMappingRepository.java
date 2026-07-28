package com.spdms.modules.activity.repository;

import com.spdms.entity.StageActivityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageActivityMappingRepository extends JpaRepository<StageActivityMapping, Long> {
    List<StageActivityMapping> findByStageId(Long stageId);
    Optional<StageActivityMapping> findByStageIdAndActivityId(Long stageId, Long activityId);
    void deleteByStageIdAndActivityId(Long stageId, Long activityId);
    void deleteByActivityId(Long activityId);
    boolean existsByStageIdAndActivityId(Long stageId, Long activityId);
}
