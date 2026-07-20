package com.spdms.modules.activity.repository;

import com.spdms.entity.ActivitySubgroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivitySubgroupRepository extends JpaRepository<ActivitySubgroup, Long> {
    List<ActivitySubgroup> findByStageId(Long stageId);
    long countByAssignedDepartmentId(Long departmentId);
}
