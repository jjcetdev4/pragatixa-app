package com.spdms.repository;

import com.spdms.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    long countByTeamId(Long teamId);
    boolean existsByTeamId(Long teamId);
    boolean existsByTeamIdAndStudentId(Long teamId, Long studentId);
    java.util.List<TeamMember> findByTeamId(Long teamId);
    
    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM TeamMember tm WHERE tm.student.studentId = :studentId AND tm.team.assignment.activity.id = :activityId) THEN true ELSE false END")
    boolean existsByStudentIdAndActivityId(String studentId, Long activityId);
}
