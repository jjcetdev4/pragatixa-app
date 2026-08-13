package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.XpTransaction;
import jjcet.PragatiX.modules.student.dto.StageXpSummary;
import jjcet.PragatiX.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StageXpSummaryService {

    private final XpTransactionRepository xpTransactionRepository;

    public StageXpSummaryService(XpTransactionRepository xpTransactionRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
    }

    public StageXpSummary getStageXp(Long studentId, Integer stageOrder) {
        if (stageOrder == null) {
            return new StageXpSummary(0, 0, 0, 0);
        }

        List<XpTransaction> transactions = xpTransactionRepository.findByStudentIdAndStageAndStatus(studentId,
                stageOrder, "APPROVED");

        int totalXp = 0;
        int mustXp = 0;
        int individualXp = 0;
        int groupXp = 0;

        for (XpTransaction tx : transactions) {
            int points = tx.getXpPoints();
            totalXp += points;

            jjcet.PragatiX.entity.Activity activity = tx.getActivity();
            if (activity != null) {
                // Use domain helper methods for classification to avoid string parsing
                if (activity.isMustXpEligible()) {
                    mustXp += points;
                }

                if (activity.isIndividualXpEligible()) {
                    individualXp += points;
                }

                if (activity.isGroupXpEligible()) {
                    groupXp += points;
                }
            }
        }

        return new StageXpSummary(totalXp, mustXp, individualXp, groupXp);
    }
}
