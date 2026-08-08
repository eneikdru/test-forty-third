package com.eneik.generated;

import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import com.jayway.jsonpath.JsonPath;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Automated test suite verifying the Document Feedback mechanisms (Comments and Actualization requests).
 * Strictly complies with the BARCAN-TAG-06 (QA Verification / Deontic Consistency) role principles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentFeedbackQaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    private UUID documentId;
    private String documentTitle = "Инструкция по технике безопасности";

    @BeforeEach
    public void setUp() {
        // Reset providers for absolute determinism (anti-nondeterminism)
        idProvider.reset();
        timeProvider.reset();

        // Clear tables and mock notification system
        jdbcTemplate.update("DELETE FROM document_comments");
        jdbcTemplate.update("DELETE FROM document_actualization_requests");
        jdbcTemplate.update("DELETE FROM analytics_events");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");
        notificationDispatcher.clear();

        // Create a test category and document
        Category category = new Category(UUID.randomUUID(), "Инструкции");
        categoryRepository.save(category);

        documentId = UUID.randomUUID();
        Document document = new Document(documentId, category, documentTitle, "Инструкция по технике безопасности для сотрудников лаборатории.");
        document.setDocumentType("Procedure");
        document.setAcademicYear("infinite");
        document.setProgram("both");
        document.setProcess("other");
        document.setStatus("ACTIVE");
        document.setCreatedAt(LocalDateTime.of(2026, 8, 8, 12, 0, 0));
        document.setUpdatedAt(LocalDateTime.of(2026, 8, 8, 12, 0, 0));
        documentRepository.save(document);
    }

    /**
     * Given the patched document view,
     * When E2E/Integration tests simulate a user submitting a comment,
     * Then the system successfully processes and records the submission without errors,
     * and dispatches a properly escaped Telegram notification to content managers.
     */
    @Test
    public void testSubmitCommentSuccessfully() throws Exception {
        // Setup deterministic time and IDs
        LocalDateTime commentTime = LocalDateTime.of(2026, 8, 8, 14, 30, 0);
        timeProvider.setFixedDateTime(commentTime);
        UUID commentUuid = UUID.randomUUID();
        idProvider.setFixedUuid(commentUuid);
        String notifId = "notif_comment_test_123";
        idProvider.setFixedStringId(notifId);

        String commentText = "Это важный комментарий о правилах пожарной безопасности. Пожалуйста, обновите пункт 3.1!";

        // POST request to submit a comment
        String response = mockMvc.perform(post("/api/documents/" + documentId + "/comments")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + commentText + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(commentUuid.toString())))
                .andExpect(jsonPath("$.userName", is("Teacher")))
                .andExpect(jsonPath("$.text", is(commentText)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Verify the comment is recorded in memory and returned by GET endpoint
        mockMvc.perform(get("/api/documents/" + documentId + "/comments")
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(commentUuid.toString())))
                .andExpect(jsonPath("$[0].userName", is("Teacher")))
                .andExpect(jsonPath("$[0].text", is(commentText)));

        // Verify Telegram notification dispatcher has been notified with properly formatted and escaped MarkdownV2
        List<TelegramNotificationRequest> dispatchedTelegram = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, dispatchedTelegram.size());

        TelegramNotificationRequest request = dispatchedTelegram.get(0);
        assertEquals(notifId, request.getNotificationId());
        assertEquals("comment.added", request.getEventType());
        assertEquals("@cniiep_edu_updates", request.getTargetId());
        assertEquals("markdown_v2", request.getMessageFormat());

        TelegramNotificationRequest.PayloadDetails payload = request.getPayload();
        assertNotNull(payload);
        assertEquals(documentId.toString(), payload.getDocumentId());
        assertEquals(documentTitle, payload.getTitle());
        assertEquals("комментарий", payload.getActionType());
        assertEquals("Teacher", payload.getAuthorName());
        assertEquals("Добавлен комментарий: " + commentText, payload.getUpdateSummary());

        // Verify rendered message contains correct Russian phrasing and escaping
        String renderedMsg = request.getRenderedMessage();
        assertNotNull(renderedMsg);
        assertTrue(renderedMsg.contains("💬 *Новый комментарий к документу в Базе Знаний*"));
        // Check escaping of dot '.' and exclamation mark '!'
        assertTrue(renderedMsg.contains("Инструкция по технике безопасности"));
        assertTrue(renderedMsg.contains("Это важный комментарий о правилах пожарной безопасности\\. Пожалуйста, обновите пункт 3\\.1\\!"));
    }

    /**
     * Given the patched document view,
     * When E2E/Integration tests simulate a user submitting an actualization request,
     * Then the system successfully processes and records the request,
     * and dispatches a properly escaped Telegram notification to content managers.
     */
    @Test
    public void testSubmitActualizationRequestSuccessfully() throws Exception {
        // Setup deterministic time and IDs
        LocalDateTime requestTime = LocalDateTime.of(2026, 8, 8, 15, 0, 0);
        timeProvider.setFixedDateTime(requestTime);
        UUID reqUuid = UUID.randomUUID();
        idProvider.setFixedUuid(reqUuid);
        String notifId = "notif_actualization_test_456";
        idProvider.setFixedStringId(notifId);

        String reasonText = "ГОСТ 12.1.004-91 был заменен на новый стандарт. Требуется срочное обновление регламента.";

        // POST request to submit an actualization request
        mockMvc.perform(post("/api/documents/" + documentId + "/actualization-requests")
                        .header("X-User-Role", "Economist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reasonText + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(reqUuid.toString())))
                .andExpect(jsonPath("$.documentId", is(documentId.toString())))
                .andExpect(jsonPath("$.reason", is(reasonText)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        // Verify Telegram notification dispatcher has been notified with properly formatted and escaped MarkdownV2
        List<TelegramNotificationRequest> dispatchedTelegram = notificationDispatcher.getDispatchedTelegram();
        assertEquals(1, dispatchedTelegram.size());

        TelegramNotificationRequest request = dispatchedTelegram.get(0);
        assertEquals(notifId, request.getNotificationId());
        assertEquals("actualization.requested", request.getEventType());
        assertEquals("@cniiep_edu_updates", request.getTargetId());
        assertEquals("markdown_v2", request.getMessageFormat());

        TelegramNotificationRequest.PayloadDetails payload = request.getPayload();
        assertNotNull(payload);
        assertEquals(documentId.toString(), payload.getDocumentId());
        assertEquals(documentTitle, payload.getTitle());
        assertEquals("запрос_актуализации", payload.getActionType());
        assertEquals("Economist", payload.getAuthorName());
        assertEquals("Запрос на актуализацию: " + reasonText, payload.getUpdateSummary());

        // Verify rendered message contains correct Russian phrasing and escaping
        String renderedMsg = request.getRenderedMessage();
        assertNotNull(renderedMsg);
        assertTrue(renderedMsg.contains("⚠️ *Запрос актуализации документа*"));
        // Check escaping of dot '.' and hyphen '-'
        assertTrue(renderedMsg.contains("ГОСТ 12\\.1\\.004\\-91 был заменен на новый стандарт\\. Требуется срочное обновление регламента\\."));
    }

    /**
     * Given invalid inputs (empty comments or actualization reasons),
     * When submitting,
     * Then the system rejects the requests with 400 Bad Request error.
     */
    @Test
    public void testSubmitInvalidFeedbackReturns400BadRequest() throws Exception {
        // 1. Empty comment
        mockMvc.perform(post("/api/documents/" + documentId + "/comments")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Comment text is required")));

        // 2. Empty actualization request
        mockMvc.perform(post("/api/documents/" + documentId + "/actualization-requests")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Actualization reason is required")));
    }

    /**
     * Given a request on a non-existent document ID,
     * When submitting comments or actualization requests,
     * Then the system returns 404 Not Found error.
     */
    @Test
    public void testSubmitFeedbackOnNonExistentDocumentReturns404NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // 1. Comment
        mockMvc.perform(post("/api/documents/" + nonExistentId + "/comments")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Нормальный коммент\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Document not found")));

        // 2. Actualization Request
        mockMvc.perform(post("/api/documents/" + nonExistentId + "/actualization-requests")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Нормальная причина\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Document not found")));
    }

    /**
     * Given missing credentials,
     * When submitting comments or actualization requests,
     * Then the system returns 401 Unauthorized.
     */
    @Test
    public void testSubmitFeedbackWithoutRoleReturns401Unauthorized() throws Exception {
        // No header X-User-Role
        mockMvc.perform(post("/api/documents/" + documentId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Тест 401\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Missing or invalid credentials")));
    }
}
