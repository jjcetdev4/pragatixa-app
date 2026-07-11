package com.spdms.repository;

import com.spdms.entity.ActivityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStageRepository extends JpaRepository<ActivityStage, Long> {
    Optional<ActivityStage> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<ActivityStage> findByIsActiveTrue();
}
