package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "document_schema_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "schema_tag_id")
    )
    private Set<SchemaTag> schemaTags = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DocumentVersion> versions = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType = DocumentType.Other;

    @Embedded
    private AcademicYear academicYear = new AcademicYear("infinite");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PROJECT;

    @Column(nullable = false)
    private String program = "both";

    @Column(nullable = false)
    private String process = "other";

    @Column(name = "approval_date")
    private java.time.LocalDate approvalDate;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "responsible_name")
    private String responsibleName;

    @Column(name = "responsible_title")
    private String responsibleTitle;

    @Column(name = "responsible_unit")
    private String responsibleUnit;

    @Column(name = "decommissioned_at")
    private LocalDateTime decommissionedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_document_id")
    private Document successorDocument;

    public Document() {}

    public Document(UUID id, Category category, String title, String description) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public String getDocumentType() {
        return documentType != null ? documentType.name() : "Other";
    }

    public void setDocumentType(String documentType) {
        if (documentType == null) {
            this.documentType = DocumentType.Other;
        } else {
            try {
                this.documentType = DocumentType.valueOf(documentType);
            } catch (IllegalArgumentException e) {
                for (DocumentType t : DocumentType.values()) {
                    if (t.name().equalsIgnoreCase(documentType)) {
                        this.documentType = t;
                        return;
                    }
                }
                this.documentType = DocumentType.Other;
            }
        }
    }

    public String getAcademicYear() {
        return academicYear != null ? academicYear.getValue() : "infinite";
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = new AcademicYear(academicYear);
    }

    public String getStatus() {
        return status != null ? status.name() : "PROJECT";
    }

    public void setStatus(String status) {
        if (status == null) {
            this.status = DocumentStatus.PROJECT;
        } else {
            try {
                this.status = DocumentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                for (DocumentStatus s : DocumentStatus.values()) {
                    if (s.name().equalsIgnoreCase(status)) {
                        this.status = s;
                        return;
                    }
                }
                this.status = DocumentStatus.PROJECT;
            }
        }
    }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getProcess() { return process; }
    public void setProcess(String process) { this.process = process; }

    public java.time.LocalDate getApprovalDate() { return approvalDate; }
    public void setApprovalDate(java.time.LocalDate approvalDate) { this.approvalDate = approvalDate; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }

    public String getResponsibleTitle() { return responsibleTitle; }
    public void setResponsibleTitle(String responsibleTitle) { this.responsibleTitle = responsibleTitle; }

    public String getResponsibleUnit() { return responsibleUnit; }
    public void setResponsibleUnit(String responsibleUnit) { this.responsibleUnit = responsibleUnit; }

    public LocalDateTime getDecommissionedAt() { return decommissionedAt; }
    public void setDecommissionedAt(LocalDateTime decommissionedAt) { this.decommissionedAt = decommissionedAt; }

    public Document getSuccessorDocument() { return successorDocument; }
    public void setSuccessorDocument(Document successorDocument) { this.successorDocument = successorDocument; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<SchemaTag> getSchemaTags() { return schemaTags; }
    public void setSchemaTags(Set<SchemaTag> schemaTags) { this.schemaTags = schemaTags; }
    public Set<DocumentVersion> getVersions() { return versions; }
    public void setVersions(Set<DocumentVersion> versions) { this.versions = versions; }
}
