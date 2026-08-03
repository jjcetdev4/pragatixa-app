package com.spdms.repository;

import com.spdms.entity.CustomFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomFrequencyRepository extends JpaRepository<CustomFrequency, Long> {
    Optional<CustomFrequency> findByNameIgnoreCase(String name);
}
