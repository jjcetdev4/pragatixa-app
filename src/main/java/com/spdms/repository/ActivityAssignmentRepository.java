package com.spdms.repository;

import com.spdms.entity.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Long> {
    List<ActivityAssignment> findByActivityId(Long activityId);
    Optional<ActivityAssignment> findByActivityIdAndSectionId(Long activityId, Long sectionId);
    Optional<ActivityAssignment> findByActivityIdAndSectionIsNull(Long activityId);
}
