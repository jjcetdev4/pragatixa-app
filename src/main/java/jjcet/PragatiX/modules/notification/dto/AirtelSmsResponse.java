package jjcet.PragatiX.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirtelSmsResponse {

    @JsonProperty("messageRequestId")
    @JsonAlias({"requestId", "messageReqId", "id", "msgId"})
    private String messageRequestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusCode")
    @JsonAlias({"code", "status_code"})
    private Integer statusCode;

    @JsonProperty("desc")
    @JsonAlias({"description", "reason", "message"})
    private String desc;

    @JsonProperty("error")
    private String error;

    public AirtelSmsResponse() {
    }

    public String getMessageRequestId() {
        return messageRequestId;
    }

    public void setMessageRequestId(String messageRequestId) {
        this.messageRequestId = messageRequestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSuccessful() {
        if (statusCode != null && (statusCode == 200 || statusCode == 201 || statusCode == 202)) {
            return true;
        }
        if ("ACCEPTED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) {
            return true;
        }
        return messageRequestId != null && !messageRequestId.trim().isEmpty() && !"FAILED".equalsIgnoreCase(status);
    }
}
