package com.spdms.repository;

import com.spdms.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreakRepository extends JpaRepository<Streak, Long> {
    List<Streak> findByStudentStudentId(String regNo);
    Optional<Streak> findByStudentStudentIdAndStreakType(String regNo, String streakType);
}
