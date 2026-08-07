package com.eneik.generated.service;

import com.eneik.generated.dto.MaxNotificationRequest;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.UserNotificationPreference;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.UserNotificationPreferenceRepository;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationService {

    private final DocumentRepository documentRepository;
    private final UserNotificationPreferenceRepository userNotificationPreferenceRepository;
    private final IdProvider idProvider;
    private final TimeProvider timeProvider;
    private final NotificationDispatcher notificationDispatcher;

    public NotificationService(DocumentRepository documentRepository,
                               UserNotificationPreferenceRepository userNotificationPreferenceRepository,
                               IdProvider idProvider,
                               TimeProvider timeProvider,
                               NotificationDispatcher notificationDispatcher) {
        this.documentRepository = documentRepository;
        this.userNotificationPreferenceRepository = userNotificationPreferenceRepository;
        this.idProvider = idProvider;
        this.timeProvider = timeProvider;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional(readOnly = true)
    public void triggerQuarterlyReview(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));

        String categoryName = (doc.getCategory() != null) ? doc.getCategory().getName() : "Нормативная база";
        String notificationId = idProvider.generateNotificationId();
        String directLink = "https://kb.crie.ru/documents/" + doc.getId();

        String authorName = "Администрация ЦНИИ";
        if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
            DocumentVersion latest = doc.getVersions().stream()
                    .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()))
                    .orElse(null);
            if (latest != null && latest.getAuthorName() != null) {
                authorName = latest.getAuthorName();
            }
        }

        String updateSummary = "Ежеквартальный пересмотр документа.";

        TelegramNotificationRequest request = new TelegramNotificationRequest();
        request.setNotificationId(notificationId);
        request.setEventType("document.updated");
        request.setRecipientType("channel_or_chat");
        request.setTargetId("@cniiep_edu_updates");
        request.setTemplateLanguage("ru");
        request.setMessageFormat("markdown_v2");

        TelegramNotificationRequest.PayloadDetails payload = new TelegramNotificationRequest.PayloadDetails();
        payload.setDocumentId(doc.getId().toString());
        payload.setTitle(doc.getTitle());
        payload.setActionType("обновление");
        payload.setCategory(categoryName);
        payload.setAuthorName(authorName);
        payload.setUpdateSummary(updateSummary);
        payload.setDirectLink(directLink);
        payload.setFileSize("2.4 MB");
        payload.setFileType("PDF");
        request.setPayload(payload);

        // Render message according to ADR-001 format
        String escapedTitle = doc.getTitle().replace(".", "\\.").replace("-", "\\-").replace("'", "\\'");
        String escapedLink = directLink.replace(".", "\\.").replace("-", "\\-");
        String escapedAuthor = authorName.replace(".", "\\.").replace("-", "\\-");
        String escapedSummary = updateSummary.replace(".", "\\.").replace("-", "\\-");
        String escapedCategory = categoryName.replace(".", "\\.").replace("-", "\\-");

        String renderedMessage = "🔔 *Новый документ в Базе Знаний ЦНИИ Эпидемиологии*\n\n" +
                "📅 *Раздел:* " + escapedCategory + "\n" +
                "📝 *Документ:* [" + escapedTitle + "](" + escapedLink + ")\n" +
                "⚙️ *Действие:* обновление\n" +
                "✍️ *Автор:* " + escapedAuthor + "\n" +
                "📎 *Формат:* PDF \\(2\\.4 MB\\)\n\n" +
                "💬 *Что изменилось:*\n" +
                escapedSummary;
        request.setRenderedMessage(renderedMessage);

        // Dispatch via decoupled dispatcher directly within JVM
        notificationDispatcher.dispatchTelegram(request);
    }

    @Transactional(readOnly = true)
    public void triggerNewVersionPublished(UUID documentId, Integer versionNumber) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));

        DocumentVersion version = null;
        if (doc.getVersions() != null) {
            version = doc.getVersions().stream()
                    .filter(v -> v.getVersionNumber().equals(versionNumber))
                    .findFirst()
                    .orElse(null);
        }

        String changesSummary = (version != null && version.getChangesSummary() != null)
                ? version.getChangesSummary()
                : "Новая версия документа.";

        List<UserNotificationPreference> subscribers = userNotificationPreferenceRepository.findByNotifyOnDocumentUpdateTrue();

        for (UserNotificationPreference subscriber : subscribers) {
            if (subscriber.getMaxChatId() != null && !subscriber.getMaxChatId().trim().isEmpty()) {
                String notificationId = idProvider.generateNotificationId();

                MaxNotificationRequest request = new MaxNotificationRequest();
                request.setNotificationId(notificationId);
                request.setEventType("document.new_version");
                request.setRecipientId(subscriber.getMaxChatId());
                request.setDocumentId(doc.getId().toString());
                request.setTitle(doc.getTitle());
                request.setVersionNumber(versionNumber);
                request.setChangesSummary(changesSummary);

                String renderedMessage = "Уведомление Max: Опубликована новая версия документа \"" + doc.getTitle() +
                        "\" (Версия " + versionNumber + "). Изменения: " + changesSummary;
                request.setRenderedMessage(renderedMessage);

                // Dispatch via decoupled dispatcher directly within JVM
                notificationDispatcher.dispatchMax(request);
            }
        }
    }
}
