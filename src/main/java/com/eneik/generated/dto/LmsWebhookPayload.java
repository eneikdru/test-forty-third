package com.eneik.generated.dto;

import java.util.Map;
import java.util.UUID;

public class LmsWebhookPayload {

    private String provider;
    private String eventType;
    private UUID documentId;
    private Map<String, Object> payload;

    public LmsWebhookPayload() {}

    public LmsWebhookPayload(String provider, String eventType, UUID documentId, Map<String, Object> payload) {
        this.provider = provider;
        this.eventType = eventType;
        this.documentId = documentId;
        this.payload = payload;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
