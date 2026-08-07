package com.eneik.generated.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TelegramNotificationRequest {
    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("recipient_type")
    private String recipientType;

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("template_language")
    private String templateLanguage = "ru";

    @JsonProperty("message_format")
    private String messageFormat = "markdown_v2";

    @JsonProperty("payload")
    private PayloadDetails payload;

    @JsonProperty("rendered_message")
    private String renderedMessage;

    public TelegramNotificationRequest() {}

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTemplateLanguage() { return templateLanguage; }
    public void setTemplateLanguage(String templateLanguage) { this.templateLanguage = templateLanguage; }
    public String getMessageFormat() { return messageFormat; }
    public void setMessageFormat(String messageFormat) { this.messageFormat = messageFormat; }
    public PayloadDetails getPayload() { return payload; }
    public void setPayload(PayloadDetails payload) { this.payload = payload; }
    public String getRenderedMessage() { return renderedMessage; }
    public void setRenderedMessage(String renderedMessage) { this.renderedMessage = renderedMessage; }

    public static class PayloadDetails {
        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("action_type")
        private String actionType;

        @JsonProperty("category")
        private String category;

        @JsonProperty("author_name")
        private String authorName;

        @JsonProperty("update_summary")
        private String updateSummary;

        @JsonProperty("direct_link")
        private String directLink;

        @JsonProperty("file_size")
        private String fileSize;

        @JsonProperty("file_type")
        private String fileType;

        public PayloadDetails() {}

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getUpdateSummary() { return updateSummary; }
        public void setUpdateSummary(String updateSummary) { this.updateSummary = updateSummary; }
        public String getDirectLink() { return directLink; }
        public void setDirectLink(String directLink) { this.directLink = directLink; }
        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
    }
}
