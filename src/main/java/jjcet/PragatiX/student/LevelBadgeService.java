package jjcet.PragatiX.student;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Badge;
import jjcet.PragatiX.entity.Level;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentBadge;
import jjcet.PragatiX.modules.student.dto.response.StudentBadgeResponse;
import jjcet.PragatiX.repository.BadgeRepository;
import jjcet.PragatiX.repository.LevelRepository;
import jjcet.PragatiX.modules.student.repository.StudentBadgeRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
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

    public Optional<Level> getCurrentLevelForStudent(String regNo) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return Optional.empty();
        }
        int totalXp = studentOpt.get().getTotalXp();
        return levelRepository.findAll().stream()
                .filter(lvl -> totalXp >= lvl.getXpMin() && totalXp <= lvl.getXpMax())
                .findFirst();
    }

    public List<Badge> getAllBadges() {
        return badgeRepository.findByDeletedFalse();
    }

    public List<StudentBadgeResponse> getBadgesForStudent(String regNo) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return List.of();
        }
        return studentBadgeRepository.findByStudentId(studentOpt.get().getId()).stream()
                .map(StudentBadgeResponse::new)
                .toList();
    }

    @Transactional
    public ApiResponse<StudentBadgeResponse> submitBadgeClaim(String regNo, String badgeName, String evidenceUrl) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Optional<Badge> badgeOpt = badgeRepository.findByNameAndDeletedFalse(badgeName);
        if (badgeOpt.isEmpty()) {
            return ApiResponse.error("Badge '" + badgeName + "' not found");
        }

        Student student = studentOpt.get();
        Badge badge = badgeOpt.get();

        if (badge.isProofRequired() && (evidenceUrl == null || evidenceUrl.trim().isEmpty())) {
            return ApiResponse.error("Proof link is required for this badge");
        }

        List<StudentBadge> existingClaims = studentBadgeRepository.findByStudentIdAndBadgeId(student.getId(),
                badge.getId());
        for (StudentBadge existingClaim : existingClaims) {
            if ("APPROVED".equalsIgnoreCase(existingClaim.getStatus())) {
                return ApiResponse.error("You have already earned this badge.");
            }
            if ("PENDING".equalsIgnoreCase(existingClaim.getStatus())) {
                return ApiResponse.error("Your claim for this badge is already pending.");
            }
        }

        StudentBadge claim = StudentBadge.builder()
                .student(student)
                .badge(badge)
                .evidenceUrl(evidenceUrl != null ? evidenceUrl.trim() : null)
                .status("PENDING")
                .awardedAt(LocalDateTime.now())
                .build();

        StudentBadge saved = studentBadgeRepository.save(claim);
        return ApiResponse.ok("Badge claim submitted successfully", new StudentBadgeResponse(saved));
    }

    @Transactional
    public ApiResponse<StudentBadgeResponse> approveBadgeClaim(Long claimId, String approvedBy) {
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
        return ApiResponse.ok("Badge claim approved successfully", new StudentBadgeResponse(saved));
    }

    @Transactional
    public ApiResponse<StudentBadgeResponse> rejectBadgeClaim(Long claimId, String rejectedBy) {
        Optional<StudentBadge> claimOpt = studentBadgeRepository.findById(claimId);
        if (claimOpt.isEmpty()) {
            return ApiResponse.error("Badge claim not found");
        }

        StudentBadge claim = claimOpt.get();
        if ("APPROVED".equalsIgnoreCase(claim.getStatus())) {
            return ApiResponse.error("Cannot reject an already approved badge");
        }
        if ("REJECTED".equalsIgnoreCase(claim.getStatus())) {
            return ApiResponse.error("Badge claim is already rejected");
        }

        claim.setStatus("REJECTED");
        claim.setApprovedBy(rejectedBy); // Overloading this field to store who rejected
        claim.setAwardedAt(LocalDateTime.now());

        StudentBadge saved = studentBadgeRepository.save(claim);
        return ApiResponse.ok("Badge claim rejected successfully", new StudentBadgeResponse(saved));
    }

    public List<StudentBadgeResponse> getPendingBadgeClaims() {
        return studentBadgeRepository.findByStatus("PENDING").stream()
                .map(StudentBadgeResponse::new)
                .toList();
    }
}
