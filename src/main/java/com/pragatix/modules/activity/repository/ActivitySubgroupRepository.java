package com.pragatix.modules.activity.repository;

import com.pragatix.entity.ActivitySubgroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivitySubgroupRepository extends JpaRepository<ActivitySubgroup, Long> {
    List<ActivitySubgroup> findByStageId(Long stageId);

    java.util.Optional<ActivitySubgroup> findByStageIdAndNameIgnoreCase(Long stageId, String name);

    java.util.Optional<ActivitySubgroup> findByStageIdAndCategoryIgnoreCase(Long stageId, String category);

    long countByAssignedDepartmentId(Long departmentId);
}
