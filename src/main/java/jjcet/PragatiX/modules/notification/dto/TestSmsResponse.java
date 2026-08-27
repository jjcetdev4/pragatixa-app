package jjcet.PragatiX.modules.notification.dto;

public class TestSmsResponse {

    private boolean success;
    private String provider;
    private String messageRequestId;
    private String maskedDestination;
    private String status;
    private String note;

    public TestSmsResponse() {
    }

    public TestSmsResponse(boolean success, String provider, String messageRequestId, String maskedDestination, String status, String note) {
        this.success = success;
        this.provider = provider;
        this.messageRequestId = messageRequestId;
        this.maskedDestination = maskedDestination;
        this.status = status;
        this.note = note;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessageRequestId() {
        return messageRequestId;
    }

    public void setMessageRequestId(String messageRequestId) {
        this.messageRequestId = messageRequestId;
    }

    public String getMaskedDestination() {
        return maskedDestination;
    }

    public void setMaskedDestination(String maskedDestination) {
        this.maskedDestination = maskedDestination;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
