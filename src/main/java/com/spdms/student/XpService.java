package com.spdms.student;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Streak;
import com.spdms.entity.XpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class XpService {

    private final XpQueryService xpQueryService;
    private final XpCommandService xpCommandService;

    public XpService(XpQueryService xpQueryService, XpCommandService xpCommandService) {
        this.xpQueryService = xpQueryService;
        this.xpCommandService = xpCommandService;
    }

    public Map<String, Integer> getXpSummary(String studentId) {
        return xpQueryService.getXpSummary(studentId);
    }

    public Page<XpTransaction> getXpHistory(String studentId, int page, int size) {
        return xpQueryService.getXpHistory(studentId, page, size);
    }

    public List<Streak> getStudentStreaks(String studentId) {
        return xpQueryService.getStudentStreaks(studentId);
    }

    public ApiResponse<XpTransaction> submitXpClaim(String studentId, String category, String activityName, int xpPoints, String evidenceUrl) {
        return xpCommandService.submitXpClaim(studentId, category, activityName, xpPoints, evidenceUrl);
    }

    public ApiResponse<XpTransaction> approveXpClaim(Long txId, String approvedBy) {
        return xpCommandService.approveXpClaim(txId, approvedBy);
    }

    public ApiResponse<XpTransaction> rejectXpClaim(Long txId, String approvedBy) {
        return xpCommandService.rejectXpClaim(txId, approvedBy);
    }

    public ApiResponse<XpTransaction> logViolation(String studentId, String violationType, int xpPenalty, String appliedBy, String description) {
        return xpCommandService.logViolation(studentId, violationType, xpPenalty, appliedBy, description);
    }
}
