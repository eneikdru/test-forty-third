package com.eneik.generated;

import com.eneik.generated.dto.EiosRoleSyncRequest;
import com.eneik.generated.dto.LmsWebhookPayload;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.service.NotificationService;
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
public class QaIntegrationsVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentLmsMetadataRepository lmsMetadataRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    private final RestTemplate restTemplate = new RestTemplate();
    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port;
        notificationDispatcher.clear();
        userRoleRepository.deleteAll();
        lmsMetadataRepository.deleteAll();
        documentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    public void testEiosRolesSyncIntegrationSuccess() {
        UUID userId = UUID.randomUUID();
        List<String> roles = Arrays.asList("CONTENT_MANAGER", "TEACHER");

        EiosRoleSyncRequest requestPayload = new EiosRoleSyncRequest(userId, roles);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer INTERNAL_SERVICE_KEY");

        HttpEntity<EiosRoleSyncRequest> entity = new HttpEntity<>(requestPayload, headers);

        String syncUrl = baseUrl + "/api/v1/integrations/eios/auth/sync";
        ResponseEntity<Map> response = restTemplate.postForEntity(syncUrl, entity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertEquals(2, response.getBody().get("syncedRolesCount"));

        // Verify roles are persisted in the database
        List<UserRole> persistedRoles = userRoleRepository.findByUserId(userId);
        assertEquals(2, persistedRoles.size());
        assertTrue(persistedRoles.stream().anyMatch(r -> r.getRoleName().equals("CONTENT_MANAGER")));
        assertTrue(persistedRoles.stream().anyMatch(r -> r.getRoleName().equals("TEACHER")));
    }

    @Test
    public void testEiosRolesSyncUnauthorized() {
        UUID userId = UUID.randomUUID();
        EiosRoleSyncRequest requestPayload = new EiosRoleSyncRequest(userId, Collections.singletonList("HR"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer WRONG_KEY");

        HttpEntity<EiosRoleSyncRequest> entity = new HttpEntity<>(requestPayload, headers);

        String syncUrl = baseUrl + "/api/v1/integrations/eios/auth/sync";

        assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.postForEntity(syncUrl, entity, Map.class);
        });
    }

    @Test
    public void testLmsWebhookCallbackUpdatesMetadata() {
        // 1. Create a Category
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Нормативные документы");
        categoryRepository.save(category);

        // 2. Create a Document
        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Положение о практике ординаторов", "Desc");
        documentRepository.save(doc);

        // 3. Fire mock LMS Webhook
        Map<String, Object> innerPayload = new HashMap<>();
        innerPayload.put("course_id", "101");
        innerPayload.put("status", "synced");

        LmsWebhookPayload webhookPayload = new LmsWebhookPayload("Teachbase", "document.updated", documentId, innerPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer INTERNAL_SERVICE_KEY");

        HttpEntity<LmsWebhookPayload> entity = new HttpEntity<>(webhookPayload, headers);

        String webhookUrl = baseUrl + "/api/v1/integrations/lms/webhooks";
        ResponseEntity<Map> response = restTemplate.postForEntity(webhookUrl, entity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));

        // 4. Verify Document LMS Metadata was inserted/updated in the database
        List<DocumentLmsMetadata> lmsMetaList = lmsMetadataRepository.findByDocumentId(documentId);
        assertEquals(1, lmsMetaList.size());
        DocumentLmsMetadata meta = lmsMetaList.get(0);
        assertEquals("Teachbase", meta.getLmsProvider());
        assertTrue(meta.getMetadataJson().contains("\"course_id\":\"101\""));
    }

    @Test
    public void testDocumentUpdateTriggersTelegramNotification() {
        // 1. Create a Category
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Положение о стипендиях");
        categoryRepository.save(category);

        // 2. Create a Document
        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Положение о стипендиальном обеспечении 2026", "Test stipends");

        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFileUrl("https://kb.crie.ru/files/v1.pdf");
        version.setFileType("PDF");
        version.setStatus("ACTIVE");
        version.setAuthorName("Макаров М.М.");
        version.setChangesSummary("Первоначальное наполнение.");
        doc.setVersions(Set.of(version));

        documentRepository.save(doc);

        // 3. Trigger document update via controller endpoint (which triggers telegram dispatch)
        String triggerUrl = baseUrl + "/api/v1/notifications/trigger/quarterly-review?documentId=" + documentId;
        ResponseEntity<Map> response = restTemplate.postForEntity(triggerUrl, null, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 4. Verify Telegram Mock API / dispatcher received the correct payload
        List<TelegramNotificationRequest> dispatched = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, dispatched.size());

        TelegramNotificationRequest request = dispatched.get(0);
        assertEquals("document.updated", request.getEventType());
        assertEquals("@cniiep_edu_updates", request.getTargetId());

        TelegramNotificationRequest.PayloadDetails payload = request.getPayload();
        assertNotNull(payload);
        assertEquals(documentId.toString(), payload.getDocumentId());
        assertEquals("Положение о стипендиальном обеспечении 2026", payload.getTitle());
        assertEquals("Положение о стипендиях", payload.getCategory());
        assertEquals("Макаров М.М.", payload.getAuthorName());

        String rendered = request.getRenderedMessage();
        assertNotNull(rendered);
        assertTrue(rendered.contains("🔔 *Новый документ в Базе Знаний ЦНИИ Эпидемиологии*"));
        assertTrue(rendered.contains("Положение о стипендиальном обеспечении 2026"));
    }
}
