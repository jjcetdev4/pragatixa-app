package jjcet.PragatiX.modules.activity.repository;

import jjcet.PragatiX.entity.ActivityStageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStageMappingRepository extends JpaRepository<ActivityStageMapping, Long> {
    boolean existsByStageIdAndActivityId(Long stageId, Long activityId);

    @Modifying
    @Transactional
    void deleteByActivityId(Long activityId);

    @Modifying
    @Transactional
    void deleteByStageIdAndActivityId(Long stageId, Long activityId);

    List<ActivityStageMapping> findByStageId(Long stageId);

    List<ActivityStageMapping> findByActivityId(Long activityId);

    List<ActivityStageMapping> findBySubgroupId(Long subgroupId);

    Optional<ActivityStageMapping> findByStageIdAndActivityId(Long stageId, Long activityId);
}
