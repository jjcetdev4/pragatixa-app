package com.pragatix.student;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.StreakResponse;
import com.pragatix.dto.XpTransactionDto;
import com.pragatix.entity.XpTransaction;
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

    public Map<String, Integer> getXpSummary(String regNo) {
        return xpQueryService.getXpSummary(regNo);
    }

    public Page<XpTransactionDto> getXpHistory(String regNo, int page, int size) {
        return xpQueryService.getXpHistory(regNo, page, size);
    }

    public List<StreakResponse> getStudentStreaks(String regNo) {
        return xpQueryService.getStudentStreaks(regNo);
    }

    public ApiResponse<XpTransaction> submitXpClaim(String regNo, String category, String activityName, int xpPoints,
            String evidenceUrl) {
        return xpCommandService.submitXpClaim(regNo, category, activityName, xpPoints, evidenceUrl);
    }

    public ApiResponse<XpTransaction> approveXpClaim(Long txId, String approvedBy) {
        return xpCommandService.approveXpClaim(txId, approvedBy);
    }

    public ApiResponse<XpTransaction> rejectXpClaim(Long txId, String approvedBy) {
        return xpCommandService.rejectXpClaim(txId, approvedBy);
    }

    public ApiResponse<XpTransaction> logViolation(String regNo, String violationType, int xpPenalty, String appliedBy,
            String description) {
        return xpCommandService.logViolation(regNo, violationType, xpPenalty, appliedBy, description);
    }

}
