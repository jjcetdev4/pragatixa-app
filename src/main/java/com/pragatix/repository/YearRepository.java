package com.pragatix.repository;

import com.pragatix.entity.Year;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YearRepository extends JpaRepository<Year, Long> {
    Optional<Year> findByYearNo(Byte yearNo);

    Optional<Year> findByYearName(String yearName);
}
