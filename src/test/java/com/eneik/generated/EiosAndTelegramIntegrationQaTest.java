package com.eneik.generated;

import com.eneik.generated.controller.IntegrationsController;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.CategoryRepository;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EiosAndTelegramIntegrationQaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        notificationDispatcher.clear();
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");

        idProvider.reset();
        timeProvider.reset();
    }

    /**
     * Given the EIOS sync integration,
     * When the mock webhook fires,
     * Then roles are verified in the database.
     */
    @Test
    public void testEiosSyncIntegrationSuccess() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        List<String> roles = Arrays.asList("eios_admin", "eios_teacher", "eios_student");

        IntegrationsController.EiosRoleSyncRequest payload = new IntegrationsController.EiosRoleSyncRequest();
        payload.setUserId(userId);
        payload.setRoles(roles);

        mockMvc.perform(post("/api/v1/integrations/eios/auth/sync")
                        .header("Authorization", "Bearer INTERNAL_SERVICE_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.syncedRolesCount", is(3)));

        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        assertEquals(3, userRoles.size());
        assertTrue(userRoles.stream().anyMatch(ur -> "eios_admin".equals(ur.getRoleName())));
        assertTrue(userRoles.stream().anyMatch(ur -> "eios_teacher".equals(ur.getRoleName())));
        assertTrue(userRoles.stream().anyMatch(ur -> "eios_student".equals(ur.getRoleName())));
    }

    /**
     * Given the notification system,
     * When a document update is triggered,
     * Then the Telegram mock API receives the correct payload.
     */
    @Test
    public void testNotificationSystemOnDocumentUpdate() throws Exception {
        String fixedNotifId = "notif_qa_telegram_999";
        idProvider.setFixedStringId(fixedNotifId);

        LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 7, 10, 0, 0);
        timeProvider.setFixedDateTime(fixedTime);

        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Test Category");
        categoryRepository.save(category);

        UUID documentId = UUID.randomUUID();
        Document doc = new Document(documentId, category, "Test Doc", "Desc");

        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFileUrl("https://test.url");
        version.setFileType("PDF");
        version.setStatus("ACTIVE");
        version.setAuthorName("Author Name");
        version.setChangesSummary("Changes");
        doc.setVersions(Set.of(version));

        documentRepository.save(doc);

        mockMvc.perform(post("/api/v1/notifications/trigger/quarterly-review")
                        .param("documentId", documentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        List<TelegramNotificationRequest> dispatchedTelegram = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, dispatchedTelegram.size());

        TelegramNotificationRequest telegramNotif = dispatchedTelegram.get(0);
        assertEquals(fixedNotifId, telegramNotif.getNotificationId());
        assertEquals("document.updated", telegramNotif.getEventType());
        assertEquals("channel_or_chat", telegramNotif.getRecipientType());
        assertEquals("@cniiep_edu_updates", telegramNotif.getTargetId());

        TelegramNotificationRequest.PayloadDetails payload = telegramNotif.getPayload();
        assertEquals(documentId.toString(), payload.getDocumentId());
        assertEquals("Test Doc", payload.getTitle());
        assertEquals("обновление", payload.getActionType());
        assertEquals("Test Category", payload.getCategory());
        assertEquals("Author Name", payload.getAuthorName());
        assertEquals("https://kb.crie.ru/documents/" + documentId, payload.getDirectLink());
    }
}
