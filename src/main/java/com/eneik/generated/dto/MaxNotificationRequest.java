package com.eneik.generated.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MaxNotificationRequest {
    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("event_type")
    private String eventType = "document.new_version";

    @JsonProperty("recipient_id")
    private String recipientId; // max_chat_id from user preferences

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("version_number")
    private Integer versionNumber;

    @JsonProperty("changes_summary")
    private String changesSummary;

    @JsonProperty("rendered_message")
    private String renderedMessage;

    public MaxNotificationRequest() {}

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getChangesSummary() { return changesSummary; }
    public void setChangesSummary(String changesSummary) { this.changesSummary = changesSummary; }
    public String getRenderedMessage() { return renderedMessage; }
    public void setRenderedMessage(String renderedMessage) { this.renderedMessage = renderedMessage; }
}
