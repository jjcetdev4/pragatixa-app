package jjcet.PragatiX.student;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.Streak;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.XpTransaction;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.XpEngineService;
import jjcet.PragatiX.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class XpCommandService {

    private final XpTransactionRepository xpTransactionRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;
    private final XpCalculationService xpCalculationService;
    private final XpEngineService xpEngineService;

    public XpCommandService(XpTransactionRepository xpTransactionRepository,
            StudentRepository studentRepository,
            ActivityRepository activityRepository,
            XpCalculationService xpCalculationService,
            XpEngineService xpEngineService) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.xpCalculationService = xpCalculationService;
        this.xpEngineService = xpEngineService;
    }

    @Transactional
    public ApiResponse<XpTransaction> submitXpClaim(String regNo, String category, String activityName, int xpPoints,
            String evidenceUrl) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        int allowedPoints = xpCalculationService.applyCapsAndLimits(student, activityName, xpPoints);
        boolean capApplied = allowedPoints < xpPoints;

        String resolvedCategory = category;
        Activity resolvedActivity = null;
        if (activityName != null) {
            List<Activity> activityList = activityRepository.findByActivityName(activityName);
            if (!activityList.isEmpty()) {
                resolvedActivity = activityList.get(0);
                if (resolvedActivity.getXpCategory() != null) {
                    resolvedCategory = resolvedActivity.getXpCategory();
                }
            }
        }

        int stageOrder = student.getStage();
        if (resolvedActivity != null && resolvedActivity.getStage() != null) {
            stageOrder = resolvedActivity.getStage().getDisplayOrder();
        }

        XpTransaction claim = XpTransaction.builder()
                .student(student)
                .activity(resolvedActivity)
                .category(resolvedCategory != null ? resolvedCategory.toUpperCase() : "SKILL")
                .activityName(activityName)
                .xpPoints(allowedPoints)
                .evidenceUrl(evidenceUrl)
                .submittedAt(LocalDateTime.now())
                .status("PENDING")
                .isPenalty(false)
                .capApplied(capApplied)
                .stage(stageOrder)
                .build();

        XpTransaction saved = xpTransactionRepository.save(claim);
        xpEngineService.updateStreakOnSubmission(student, activityName);

        return ApiResponse.ok("XP Claim submitted successfully", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> approveXpClaim(Long txId, String approvedBy) {
        Optional<XpTransaction> txOpt = xpTransactionRepository.findById(txId);
        if (txOpt.isEmpty()) {
            return ApiResponse.error("XP transaction not found");
        }
        XpTransaction tx = txOpt.get();
        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            return ApiResponse.error("Transaction is already processed");
        }

        tx.setStatus("APPROVED_AND_PROCESSED");
        tx.setApprovedBy(approvedBy);
        XpTransaction saved = xpTransactionRepository.save(tx);

        Student student = tx.getStudent();

        // Let XpEngineService handle the actual categorization and promotion
        xpEngineService.awardXp(student, tx.getActivity(), null, null, tx.getXpPoints(), "Approved Claim");

        return ApiResponse.ok("XP transaction approved successfully", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> rejectXpClaim(Long txId, String approvedBy) {
        Optional<XpTransaction> txOpt = xpTransactionRepository.findById(txId);
        if (txOpt.isEmpty()) {
            return ApiResponse.error("XP transaction not found");
        }
        XpTransaction tx = txOpt.get();
        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            return ApiResponse.error("Transaction is already processed");
        }

        tx.setStatus("REJECTED");
        tx.setApprovedBy(approvedBy);
        XpTransaction saved = xpTransactionRepository.save(tx);

        return ApiResponse.ok("XP transaction rejected", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> logViolation(String regNo, String violationType, int xpPenalty, String appliedBy,
            String description) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        xpEngineService.awardXp(student, null, null, null, -Math.abs(xpPenalty),
                "Violation: " + violationType + " - " + description);

        return ApiResponse.ok("Violation logged successfully. Points deducted.", null);
    }

}
