package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_lms_metadata")
public class DocumentLmsMetadata {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "lms_provider", nullable = false, length = 100)
    private String lmsProvider;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DocumentLmsMetadata() {}

    public DocumentLmsMetadata(UUID id, Document document, String lmsProvider, String externalId, String externalUrl, String metadataJson) {
        this.id = id;
        this.document = document;
        this.lmsProvider = lmsProvider;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.metadataJson = metadataJson;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getLmsProvider() { return lmsProvider; }
    public void setLmsProvider(String lmsProvider) { this.lmsProvider = lmsProvider; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
