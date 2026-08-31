package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityCategoryRepository extends JpaRepository<ActivityCategory, Long> {

    List<ActivityCategory> findByDeletedFalseOrderByNameAsc();

    List<ActivityCategory> findByDeletedFalseOrderByDisplayOrderAscNameAsc();

    Optional<ActivityCategory> findByIdAndDeletedFalse(Long id);

    Optional<ActivityCategory> findByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, Long id);
}
