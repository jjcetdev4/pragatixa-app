package jjcet.PragatiX.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AirtelSmsRequest {

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("destinationAddress")
    private String destinationAddress;

    @JsonProperty("dltTemplateId")
    private String dltTemplateId;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("sourceAddress")
    private String sourceAddress;

    public AirtelSmsRequest() {
    }

    public AirtelSmsRequest(String customerId, String destinationAddress, String dltTemplateId,
                             String entityId, String message, String messageType, String sourceAddress) {
        this.customerId = customerId;
        this.destinationAddress = destinationAddress;
        this.dltTemplateId = dltTemplateId;
        this.entityId = entityId;
        this.message = message;
        this.messageType = messageType;
        this.sourceAddress = sourceAddress;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getDltTemplateId() {
        return dltTemplateId;
    }

    public void setDltTemplateId(String dltTemplateId) {
        this.dltTemplateId = dltTemplateId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }
}
