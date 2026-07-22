package com.spdms.student;

import com.spdms.dto.StreakResponse;
import com.spdms.dto.XpTransactionDto;
import com.spdms.entity.Streak;
import com.spdms.entity.XpTransaction;
import com.spdms.repository.StreakRepository;
import com.spdms.repository.XpTransactionRepository;
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

    public XpQueryService(XpTransactionRepository xpTransactionRepository, StreakRepository streakRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.streakRepository = streakRepository;
    }

    public Map<String, Integer> getXpSummary(String regNo) {
        List<XpTransaction> txs = xpTransactionRepository.findByStudentRegNo(regNo);
        Map<String, Integer> summary = new HashMap<>();
        summary.put("ACADEMIC", 0);
        summary.put("SKILL", 0);
        summary.put("COMMUNICATION", 0);
        summary.put("LEADERSHIP", 0);
        summary.put("INNOVATION", 0);
        summary.put("PLACEMENT", 0);
        summary.put("DISCIPLINE", 0);
        summary.put("COMMUNITY", 0);
        summary.put("SPORTS", 0);
        summary.put("CULTURAL", 0);

        for (XpTransaction tx : txs) {
            if ("APPROVED".equalsIgnoreCase(tx.getStatus())) {
                String cat = tx.getCategory().toUpperCase();
                summary.put(cat, summary.getOrDefault(cat, 0) + tx.getXpPoints());
            }
        }
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
