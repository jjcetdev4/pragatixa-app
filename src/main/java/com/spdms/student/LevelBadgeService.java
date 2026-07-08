package com.spdms.student;

import com.spdms.dto.ApiResponse;
import com.spdms.entity.Badge;
import com.spdms.entity.Level;
import com.spdms.entity.Student;
import com.spdms.entity.StudentBadge;
import com.spdms.repository.BadgeRepository;
import com.spdms.repository.LevelRepository;
import com.spdms.repository.StudentBadgeRepository;
import com.spdms.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LevelBadgeService {

    private final LevelRepository levelRepository;
    private final BadgeRepository badgeRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final StudentRepository studentRepository;

    public LevelBadgeService(LevelRepository levelRepository,
                             BadgeRepository badgeRepository,
                             StudentBadgeRepository studentBadgeRepository,
                             StudentRepository studentRepository) {
        this.levelRepository = levelRepository;
        this.badgeRepository = badgeRepository;
        this.studentBadgeRepository = studentBadgeRepository;
        this.studentRepository = studentRepository;
    }

    public List<Level> getAllLevels() {
        return levelRepository.findAll();
    }

    public Optional<Level> getCurrentLevelForStudent(String studentId) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            return Optional.empty();
        }
        int score = studentOpt.get().getScore();
        return levelRepository.findAll().stream()
                .filter(lvl -> score >= lvl.getXpMin() && score <= lvl.getXpMax())
                .findFirst();
    }

    public List<Badge> getAllBadges() {
        return badgeRepository.findAll();
    }

    public List<StudentBadge> getBadgesForStudent(String studentId) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            return List.of();
        }
        return studentBadgeRepository.findByStudentId(studentOpt.get().getId());
    }

    @Transactional
    public ApiResponse<StudentBadge> submitBadgeClaim(String studentId, String badgeName, String evidenceUrl) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Optional<Badge> badgeOpt = badgeRepository.findByName(badgeName);
        if (badgeOpt.isEmpty()) {
            return ApiResponse.error("Badge '" + badgeName + "' not found");
        }

        Student student = studentOpt.get();
        Badge badge = badgeOpt.get();

        if (studentBadgeRepository.existsByStudentIdAndBadgeId(student.getId(), badge.getId())) {
            return ApiResponse.error("You have already claimed or been awarded this badge");
        }

        StudentBadge claim = StudentBadge.builder()
                .student(student)
                .badge(badge)
                .evidenceUrl(evidenceUrl)
                .status("PENDING")
                .awardedAt(LocalDateTime.now())
                .build();

        StudentBadge saved = studentBadgeRepository.save(claim);
        return ApiResponse.ok("Badge claim submitted successfully", saved);
    }

    @Transactional
    public ApiResponse<StudentBadge> approveBadgeClaim(Long claimId, String approvedBy) {
        Optional<StudentBadge> claimOpt = studentBadgeRepository.findById(claimId);
        if (claimOpt.isEmpty()) {
            return ApiResponse.error("Badge claim not found");
        }

        StudentBadge claim = claimOpt.get();
        if ("APPROVED".equalsIgnoreCase(claim.getStatus())) {
            return ApiResponse.error("Badge claim is already approved");
        }

        claim.setStatus("APPROVED");
        claim.setApprovedBy(approvedBy);
        claim.setAwardedAt(LocalDateTime.now());

        StudentBadge saved = studentBadgeRepository.save(claim);
        return ApiResponse.ok("Badge claim approved successfully", saved);
    }

    public List<StudentBadge> getPendingBadgeClaims() {
        return studentBadgeRepository.findByStatus("PENDING");
    }
}
