package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.ActivityEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityEvidenceRepository extends JpaRepository<ActivityEvidence, Long> {

    List<ActivityEvidence> findByDeletedFalseOrderByDisplayOrderAscNameAsc();

    Optional<ActivityEvidence> findByIdAndDeletedFalse(Long id);

    Optional<ActivityEvidence> findByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, Long id);

    List<ActivityEvidence> findByDeletedTrue();
}
