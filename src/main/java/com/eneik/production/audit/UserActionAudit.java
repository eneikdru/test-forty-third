package com.eneik.production.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_action_audit")
public class UserActionAudit {

    @Id
    private UUID id;

    @Column(name = "user_identity", nullable = false)
    private String userIdentity;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserActionAudit() {}

    public UserActionAudit(UUID id, String userIdentity, String actionType, String details, LocalDateTime createdAt) {
        this.id = id;
        this.userIdentity = userIdentity;
        this.actionType = actionType;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserIdentity() { return userIdentity; }
    public void setUserIdentity(String userIdentity) { this.userIdentity = userIdentity; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
