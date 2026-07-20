package com.spdms.modules.student.service;

import com.spdms.entity.XpTransaction;
import com.spdms.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentXpAggregator {

    private final XpTransactionRepository xpTransactionRepository;

    public StudentXpAggregator(XpTransactionRepository xpTransactionRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
    }

    public AggregatedXp aggregateXpForStudent(Long regNo) {
        Map<Long, Integer> xpByActivityId = new HashMap<>();
        Map<String, Integer> xpByActivityName = new HashMap<>();
        
        List<XpTransaction> allTxs = xpTransactionRepository.findByStudentIdAndStatus(regNo, "APPROVED");

        for (XpTransaction tx : allTxs) {
            if (tx.getActivity() != null) {
                xpByActivityId.merge(tx.getActivity().getId(), tx.getXpPoints(), Integer::sum);
            }
            if (tx.getActivityName() != null) {
                String baseName = tx.getActivityName();
                int idx = baseName.lastIndexOf(" (");
                if (idx != -1 && baseName.contains("Awarded by")) {
                    baseName = baseName.substring(0, idx);
                }
                String normalizedTxName = baseName.trim().toLowerCase().replaceAll("\\s+", " ").replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
                xpByActivityName.merge(normalizedTxName, tx.getXpPoints(), Integer::sum);
            }
        }
        return new AggregatedXp(xpByActivityId, xpByActivityName);
    }

    public static class AggregatedXp {
        public final Map<Long, Integer> xpByActivityId;
        public final Map<String, Integer> xpByActivityName;

        public AggregatedXp(Map<Long, Integer> xpByActivityId, Map<String, Integer> xpByActivityName) {
            this.xpByActivityId = xpByActivityId;
            this.xpByActivityName = xpByActivityName;
        }
    }
}
