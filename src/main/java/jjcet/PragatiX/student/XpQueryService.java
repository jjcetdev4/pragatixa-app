package jjcet.PragatiX.student;

import jjcet.PragatiX.dto.StreakResponse;
import jjcet.PragatiX.dto.XpTransactionDto;
import jjcet.PragatiX.entity.Streak;
import jjcet.PragatiX.entity.XpTransaction;
import jjcet.PragatiX.repository.StreakRepository;
import jjcet.PragatiX.repository.XpTransactionRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StageXpSummaryService;
import jjcet.PragatiX.modules.student.dto.StageXpSummary;
import jjcet.PragatiX.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class XpQueryService {

    private final XpTransactionRepository xpTransactionRepository;
    private final StreakRepository streakRepository;
    private final StudentRepository studentRepository;
    private final StageXpSummaryService stageXpSummaryService;

    public XpQueryService(XpTransactionRepository xpTransactionRepository, StreakRepository streakRepository,
            StudentRepository studentRepository,
            StageXpSummaryService stageXpSummaryService) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.streakRepository = streakRepository;
        this.studentRepository = studentRepository;
        this.stageXpSummaryService = stageXpSummaryService;
    }

    private Student findStudentByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new jjcet.PragatiX.modules.student.exception.StudentNotFoundException(
                    "Student identifier is required");
        }
        String idStr = identifier.trim();
        java.util.Optional<Student> opt = studentRepository.findByRegNo(idStr);
        if (opt.isPresent())
            return opt.get();

        opt = studentRepository.findBySprNo(idStr);
        if (opt.isPresent())
            return opt.get();

        opt = studentRepository.findByEmail(idStr);
        if (opt.isPresent())
            return opt.get();

        try {
            Long id = Long.parseLong(idStr);
            opt = studentRepository.findById(id);
            if (opt.isPresent())
                return opt.get();

            opt = studentRepository.findByUserId(id);
            if (opt.isPresent())
                return opt.get();
        } catch (NumberFormatException ignored) {
        }

        throw new jjcet.PragatiX.modules.student.exception.StudentNotFoundException("Student not found: " + identifier);
    }

    public Map<String, Integer> getXpSummary(String regNo) {
        Student student = findStudentByIdentifier(regNo);

        int currentStage = student.getStage();
        StageXpSummary stageXp = stageXpSummaryService.getStageXp(student.getId(), currentStage);

        Map<String, Integer> summary = new HashMap<>();
        summary.put("totalXp", student.getTotalXp());
        summary.put("groupXp", stageXp.getGroupXp());
        summary.put("individualXp", stageXp.getIndividualXp());
        summary.put("mustXp", stageXp.getMustXp());
        summary.put("stageOrder", currentStage);
        summary.put("stageTotalXp", stageXp.getTotalXp());

        return summary;
    }

    public Page<XpTransactionDto> getXpHistory(String regNo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return xpTransactionRepository.findByStudentRegNo(regNo, pageable).map(tx -> {
            XpTransactionDto dto = new XpTransactionDto();
            dto.setId(tx.getId());
            dto.setStudentRegNo(tx.getStudent() != null ? tx.getStudent().getRegNo() : null);
            dto.setActivityId(tx.getActivity() != null ? tx.getActivity().getId() : null);
            dto.setCategory(tx.getCategory());
            dto.setActivityName(tx.getActivityName());
            dto.setXpPoints(tx.getXpPoints());
            dto.setEvidenceUrl(tx.getEvidenceUrl());
            dto.setSubmittedAt(tx.getSubmittedAt());
            dto.setStatus(tx.getStatus());
            dto.setApprovedBy(tx.getApprovedBy());
            dto.setPenalty(tx.isPenalty());
            dto.setCapApplied(tx.isCapApplied());
            dto.setStage(tx.getStage());
            return dto;
        });
    }

    public List<StreakResponse> getStudentStreaks(String regNo) {
        return streakRepository.findByStudentRegNo(regNo).stream().map(streak -> {
            StreakResponse res = new StreakResponse();
            res.setCurrentStreak(streak.getCurrentStreak());
            res.setIsBroken(streak.isBroken());
            res.setLastUpdated(streak.getLastUpdated());
            res.setStreakType(streak.getStreakType());
            res.setPenaltyPerBreak(streak.getPenaltyPerBreak());
            return res;
        }).collect(Collectors.toList());
    }
}
