package com.eneik.generated;

import com.eneik.generated.dto.MaxNotificationRequest;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.service.NotificationService;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NotificationTriggerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserNotificationPreferenceRepository userNotificationPreferenceRepository;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    private final RestTemplate restTemplate = new RestTemplate();
    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port;
        idProvider.reset();
        timeProvider.reset();

        // Clear dispatcher and repository data before each test
        notificationDispatcher.clear();

        userNotificationPreferenceRepository.deleteAll();
        documentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    public void testQuarterlyReviewTriggerDispatchesToTelegram() {
        // Setup deterministic ID and Time
        String fixedNotifId = "notif_quarterly_review_12345";
        idProvider.setFixedStringId(fixedNotifId);

        LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 7, 12, 0, 0);
        timeProvider.setFixedDateTime(fixedTime);

        // 1. Create a Category
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Нормативно-правовые акты");
        categoryRepository.save(category);

        // 2. Create a Document
        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Положение о приеме на 2026-2027", "Test doc desc");

        // Add a version
        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFileUrl("https://kb.crie.ru/files/v1.pdf");
        version.setFileType("PDF");
        version.setStatus("ACTIVE");
        version.setAuthorName("Иванов И.И.");
        version.setChangesSummary("Первоначальное наполнение.");
        doc.setVersions(Set.of(version));

        documentRepository.save(doc);

        // 3. Fire quarterly review trigger via REST controller
        String triggerUrl = baseUrl + "/api/v1/notifications/trigger/quarterly-review?documentId=" + documentId;
        ResponseEntity<Map> response = restTemplate.postForEntity(triggerUrl, null, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));

        // 4. Retrieve dispatched notifications directly from the dispatcher and verify
        List<TelegramNotificationRequest> telegramList = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, telegramList.size());

        TelegramNotificationRequest telegramNotif = telegramList.get(0);
        assertEquals(fixedNotifId, telegramNotif.getNotificationId());
        assertEquals("document.updated", telegramNotif.getEventType());
        assertEquals("channel_or_chat", telegramNotif.getRecipientType());
        assertEquals("@cniiep_edu_updates", telegramNotif.getTargetId());
        assertEquals("ru", telegramNotif.getTemplateLanguage());
        assertEquals("markdown_v2", telegramNotif.getMessageFormat());

        TelegramNotificationRequest.PayloadDetails payload = telegramNotif.getPayload();
        assertNotNull(payload);
        assertEquals(documentId.toString(), payload.getDocumentId());
        assertEquals("Положение о приеме на 2026-2027", payload.getTitle());
        assertEquals("обновление", payload.getActionType());
        assertEquals("Нормативно-правовые акты", payload.getCategory());
        assertEquals("Иванов И.И.", payload.getAuthorName());
        assertEquals("Ежеквартальный пересмотр документа.", payload.getUpdateSummary());
        assertEquals("https://kb.crie.ru/documents/" + documentId, payload.getDirectLink());

        // Verify the rendered message formatting (escaping)
        String renderedMsg = telegramNotif.getRenderedMessage();
        assertNotNull(renderedMsg);
        assertTrue(renderedMsg.contains("🔔 *Новый документ в Базе Знаний ЦНИИ Эпидемиологии*"));
        assertTrue(renderedMsg.contains("Положение о приеме на 2026\\-2027"));
        assertTrue(renderedMsg.contains("Иванов И\\.И\\."));
    }

    @Test
    public void testNewVersionTriggerDispatchesToMaxSubscribers() {
        // Setup deterministic ID
        String fixedNotifId = "notif_max_new_version_999";
        idProvider.setFixedStringId(fixedNotifId);

        // 1. Create a Category
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Учебно-методические материалы");
        categoryRepository.save(category);

        // 2. Create a Document with two versions
        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Положение о ГИА и аттестации", "Test doc desc");

        DocumentVersion v1 = new DocumentVersion();
        v1.setId(UUID.randomUUID());
        v1.setDocument(doc);
        v1.setVersionNumber(1);
        v1.setFileUrl("https://kb.crie.ru/files/v1.pdf");
        v1.setFileType("PDF");
        v1.setStatus("ACTIVE");
        v1.setAuthorName("Петров П.П.");
        v1.setChangesSummary("Первая версия.");

        DocumentVersion v2 = new DocumentVersion();
        v2.setId(UUID.randomUUID());
        v2.setDocument(doc);
        v2.setVersionNumber(2);
        v2.setFileUrl("https://kb.crie.ru/files/v2.pdf");
        v2.setFileType("PDF");
        v2.setStatus("ACTIVE");
        v2.setAuthorName("Петров П.П.");
        v2.setChangesSummary("Добавлен регламент проведения апелляций.");

        doc.setVersions(Set.of(v1, v2));
        documentRepository.save(doc);

        // 3. Create user preferences: one subscriber with MAX, one subscriber without MAX, one unsubscribed
        UUID userIdSubMax = UUID.randomUUID();
        UserNotificationPreference prefSubMax = new UserNotificationPreference(
                UUID.randomUUID(), userIdSubMax, "tg_chat_1", "max_chat_abc", true
        );
        userNotificationPreferenceRepository.save(prefSubMax);

        UUID userIdSubNoMax = UUID.randomUUID();
        UserNotificationPreference prefSubNoMax = new UserNotificationPreference(
                UUID.randomUUID(), userIdSubNoMax, "tg_chat_2", null, true
        );
        userNotificationPreferenceRepository.save(prefSubNoMax);

        UUID userIdUnsub = UUID.randomUUID();
        UserNotificationPreference prefUnsub = new UserNotificationPreference(
                UUID.randomUUID(), userIdUnsub, "tg_chat_3", "max_chat_xyz", false
        );
        userNotificationPreferenceRepository.save(prefUnsub);

        // 4. Fire new version trigger via REST controller for version 2
        String triggerUrl = baseUrl + "/api/v1/notifications/trigger/new-version?documentId=" + documentId + "&versionNumber=2";
        ResponseEntity<Map> response = restTemplate.postForEntity(triggerUrl, null, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 5. Retrieve dispatched notifications directly and verify
        List<MaxNotificationRequest> maxList = notificationDispatcher.getDispatchedMax();
        // Only the subscriber with MAX should receive the alert
        assertEquals(1, maxList.size());

        MaxNotificationRequest maxNotif = maxList.get(0);
        assertEquals(fixedNotifId, maxNotif.getNotificationId());
        assertEquals("document.new_version", maxNotif.getEventType());
        assertEquals("max_chat_abc", maxNotif.getRecipientId());
        assertEquals(documentId.toString(), maxNotif.getDocumentId());
        assertEquals("Положение о ГИА и аттестации", maxNotif.getTitle());
        assertEquals(2, maxNotif.getVersionNumber());
        assertEquals("Добавлен регламент проведения апелляций.", maxNotif.getChangesSummary());

        String renderedMsg = maxNotif.getRenderedMessage();
        assertNotNull(renderedMsg);
        assertTrue(renderedMsg.contains("Опубликована новая версия документа \"Положение о ГИА и аттестации\""));
        assertTrue(renderedMsg.contains("Версия 2"));
        assertTrue(renderedMsg.contains("Добавлен регламент проведения апелляций."));
    }

    @Test
    public void testTelegramMarkdownV2Escaping() {
        // Setup deterministic ID and Time
        String fixedNotifId = "notif_escaping_9999";
        idProvider.setFixedStringId(fixedNotifId);

        LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 7, 12, 0, 0);
        timeProvider.setFixedDateTime(fixedTime);

        // 1. Create a Category with special characters
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Категория! _специальная_");
        categoryRepository.save(category);

        // 2. Create a Document with special characters
        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Положение [о] стипендиях (аспирантура) *2026*", "Test doc desc");

        // Add a version
        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFileUrl("https://kb.crie.ru/files/v1.pdf");
        version.setFileType("PDF");
        version.setStatus("ACTIVE");
        version.setAuthorName("Петров П.П. & Сидоров С.С.");
        version.setChangesSummary("Первоначальное наполнение.");
        doc.setVersions(Set.of(version));

        documentRepository.save(doc);

        // 3. Fire quarterly review trigger via REST controller
        String triggerUrl = baseUrl + "/api/v1/notifications/trigger/quarterly-review?documentId=" + documentId;
        ResponseEntity<Map> response = restTemplate.postForEntity(triggerUrl, null, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 4. Retrieve and verify that special characters are escaped in Telegram message
        List<TelegramNotificationRequest> telegramList = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, telegramList.size());

        TelegramNotificationRequest telegramNotif = telegramList.get(0);
        String renderedMsg = telegramNotif.getRenderedMessage();
        assertNotNull(renderedMsg);

        // Category name: "Категория! _специальная_" -> "Категория\! \_специальная\_"
        assertTrue(renderedMsg.contains("Категория\\! \\_специальная\\_"));

        // Title: "Положение [о] стипендиях (аспирантура) *2026*" -> "Положение \[о\] стипендиях \(аспирантура\) \*2026\*"
        assertTrue(renderedMsg.contains("Положение \\[о\\] стипендиях \\(аспирантура\\) \\*2026\\*"));
    }

    @Test
    public void testDispatchSecurityRequiresBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Empty auth header should fail with 401
        HttpEntity<TelegramNotificationRequest> emptyEntity = new HttpEntity<>(new TelegramNotificationRequest(), headers);
        assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.postForObject(baseUrl + "/api/v1/notifications/telegram/dispatch", emptyEntity, String.class);
        });

        // Wrong token should fail with 401
        headers.set("Authorization", "Bearer WRONG_KEY");
        HttpEntity<TelegramNotificationRequest> wrongEntity = new HttpEntity<>(new TelegramNotificationRequest(), headers);
        assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.postForObject(baseUrl + "/api/v1/notifications/telegram/dispatch", wrongEntity, String.class);
        });

        // Valid token should succeed (200)
        headers.set("Authorization", "Bearer INTERNAL_SERVICE_KEY");
        HttpEntity<TelegramNotificationRequest> validEntity = new HttpEntity<>(new TelegramNotificationRequest(), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/api/v1/notifications/telegram/dispatch", validEntity, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
