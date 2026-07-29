package com.pragatix.repository;

import com.pragatix.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreakRepository extends JpaRepository<Streak, Long> {
    List<Streak> findByStudentRegNo(String regNo);

    Optional<Streak> findByStudentRegNoAndStreakType(String regNo, String streakType);

    Optional<Streak> findByStudentIdAndStreakType(Long studentId, String streakType);
}
