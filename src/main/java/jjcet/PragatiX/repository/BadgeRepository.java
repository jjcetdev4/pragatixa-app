package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Optional<Badge> findByName(String name);
    Optional<Badge> findByNameAndDeletedFalse(String name);
    Optional<Badge> findByIdAndDeletedFalse(Long id);
    List<Badge> findByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);
    boolean existsByNameAndIdNotAndDeletedFalse(String name, Long id);
}

