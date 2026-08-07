package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "max_chat_id")
    private String maxChatId;

    @Column(name = "notify_on_document_update", nullable = false)
    private Boolean notifyOnDocumentUpdate = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserNotificationPreference() {}

    public UserNotificationPreference(UUID id, UUID userId, String telegramChatId, String maxChatId, Boolean notifyOnDocumentUpdate) {
        this.id = id;
        this.userId = userId;
        this.telegramChatId = telegramChatId;
        this.maxChatId = maxChatId;
        this.notifyOnDocumentUpdate = notifyOnDocumentUpdate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }
    public String getMaxChatId() { return maxChatId; }
    public void setMaxChatId(String maxChatId) { this.maxChatId = maxChatId; }
    public Boolean getNotifyOnDocumentUpdate() { return notifyOnDocumentUpdate; }
    public void setNotifyOnDocumentUpdate(Boolean notifyOnDocumentUpdate) { this.notifyOnDocumentUpdate = notifyOnDocumentUpdate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
