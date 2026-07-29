package com.pragatix.student;

import com.pragatix.entity.Student;
import com.pragatix.entity.XpTransaction;
import com.pragatix.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class XpCalculationService {

    private final XpTransactionRepository xpTransactionRepository;

    public XpCalculationService(XpTransactionRepository xpTransactionRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
    }

    public int applyCapsAndLimits(Student student, String activity, int points) {
        if (points < 0) {
            return points;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0)
                .withMinute(0);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);

        List<XpTransaction> studentTxs = xpTransactionRepository.findByStudentRegNo(student.getRegNo());

        if (activity.toLowerCase().contains("attendance")) {
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "attendance", startOfMonth);
            return Math.max(0, Math.min(points, 120 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("oral presentation")) {
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "oral presentation", startOfMonth);
            return Math.max(0, Math.min(points, 120 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("english diary")) {
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "english diary", startOfWeek);
            return Math.max(0, Math.min(points, 25 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("c programming") || activity.toLowerCase().contains("c coding")) {
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "c programming", startOfWeek)
                    + sumPointsForActivityInPeriod(studentTxs, "c coding", startOfWeek);
            return Math.max(0, Math.min(points, 50 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("library")) {
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "library", startOfWeek);
            return Math.max(0, Math.min(points, 60 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("coe lab") || activity.toLowerCase().contains("d2p lab")) {
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "coe lab", startOfWeek)
                    + sumPointsForActivityInPeriod(studentTxs, "d2p lab", startOfWeek);
            return Math.max(0, Math.min(points, 60 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("domain report")) {
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "domain report", startOfMonth);
            return Math.max(0, Math.min(points, 150 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("certificate course")) {
            int semesterTotal = sumPointsForActivityInPeriod(studentTxs, "certificate course", now.minusMonths(5));
            return Math.max(0, Math.min(points, 200 - semesterTotal));
        } else if (activity.toLowerCase().contains("resume first draft")) {
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "resume first draft");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms word")) {
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms word");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms excel")) {
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms excel");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms powerpoint")) {
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms powerpoint");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("typing 20 wpm")) {
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "typing 20 wpm");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("duolingo")) {
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "duolingo", startOfMonth);
            return Math.max(0, Math.min(points, 45 - currentMonthEarned));
        }

        return points;
    }

    private int sumPointsForActivityInPeriod(List<XpTransaction> txs, String activityKeyword, LocalDateTime since) {
        int sum = 0;
        for (XpTransaction tx : txs) {
            if (tx.getActivityName().toLowerCase().contains(activityKeyword)
                    && tx.getSubmittedAt().isAfter(since)
                    && !"REJECTED".equalsIgnoreCase(tx.getStatus())) {
                sum += tx.getXpPoints();
            }
        }
        return sum;
    }

    private boolean anyActivityClaimed(List<XpTransaction> txs, String activityKeyword) {
        for (XpTransaction tx : txs) {
            if (tx.getActivityName().toLowerCase().contains(activityKeyword)
                    && !"REJECTED".equalsIgnoreCase(tx.getStatus())) {
                return true;
            }
        }
        return false;
    }
}
