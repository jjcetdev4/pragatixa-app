package com.pragatix.repository;

import com.pragatix.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {
    Optional<Level> findByLevelNumber(int levelNumber);

    List<Level> findAllByOrderByXpMinAsc();
}
