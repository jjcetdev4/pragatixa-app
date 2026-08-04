package com.pragatix.student;

import com.pragatix.dto.StreakResponse;
import com.pragatix.dto.XpTransactionDto;
import com.pragatix.entity.Streak;
import com.pragatix.entity.XpTransaction;
import com.pragatix.repository.StreakRepository;
import com.pragatix.repository.XpTransactionRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.entity.Student;
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

    public XpQueryService(XpTransactionRepository xpTransactionRepository, StreakRepository streakRepository,
            StudentRepository studentRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.streakRepository = streakRepository;
        this.studentRepository = studentRepository;
    }

    public Map<String, Integer> getXpSummary(String regNo) {
        Student student = studentRepository.findByRegNo(regNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Map<String, Integer> summary = new HashMap<>();
        summary.put("totalXp", student.getTotalXp());
        summary.put("groupXp", student.getGroupXp());
        summary.put("individualXp", student.getIndividualXp());
        summary.put("mustXp", student.getMustXp());

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
