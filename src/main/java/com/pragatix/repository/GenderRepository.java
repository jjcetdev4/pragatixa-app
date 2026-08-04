package com.pragatix.repository;

import com.pragatix.entity.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenderRepository extends JpaRepository<Gender, Long> {
    Optional<Gender> findByGenderName(String genderName);
}
