package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.StageTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageTeamRepository extends JpaRepository<StageTeam, Long> {
    Optional<StageTeam> findByStageIdAndTeamId(Long stageId, Long teamId);

    List<StageTeam> findByStageId(Long stageId);

    List<StageTeam> findByTeamId(Long teamId);

    @org.springframework.data.jpa.repository.Query("SELECT st FROM StageTeam st JOIN FETCH st.team t WHERE st.stage.id = :stageId AND " +
            "(t.department.id = :deptId OR (t.department IS NULL AND :deptId IS NULL)) AND " +
            "(t.year = :year OR (t.year IS NULL AND :year IS NULL)) AND " +
            "(t.section.id = :secId OR (t.section IS NULL AND :secId IS NULL))")
    List<StageTeam> findByStageIdAndClass(@org.springframework.data.repository.query.Param("stageId") Long stageId,
                                          @org.springframework.data.repository.query.Param("deptId") Long deptId,
                                          @org.springframework.data.repository.query.Param("secId") Long secId,
                                          @org.springframework.data.repository.query.Param("year") String year);
}
