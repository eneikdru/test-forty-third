package com.eneik.generated;

import com.eneik.generated.controller.IntegrationsController;
import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentLmsMetadata;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.CategoryRepository;
import com.eneik.generated.repository.DocumentLmsMetadataRepository;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.eneik.generated.service.DocumentSearchService;
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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentLmsMetadataRepository documentLmsMetadataRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private DocumentSearchService documentSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID categoryId;
    private UUID documentId;

    @BeforeEach
    public void setUp() {
        // Clear tables
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM document_lms_metadata");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");

        // Seed Category
        Category category = new Category();
        categoryId = UUID.randomUUID();
        category.setId(categoryId);
        category.setName("Нормативные документы");
        categoryRepository.save(category);

        // Seed Document
        Document document = new Document();
        documentId = UUID.randomUUID();
        document.setId(documentId);
        document.setCategory(category);
        document.setTitle("Положение о практике аспирантов");
        document.setDescription("Регламентирует порядок прохождения производственной практики.");
        documentRepository.save(document);
    }

    @Test
    public void testSyncEiosRolesSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        List<String> roles = Arrays.asList("Teacher", "CONTENT_MANAGER");

        IntegrationsController.EiosRoleSyncRequest payload = new IntegrationsController.EiosRoleSyncRequest();
        payload.setUserId(userId);
        payload.setRoles(roles);

        mockMvc.perform(post("/api/v1/integrations/eios/auth/sync")
                        .header("Authorization", "Bearer INTERNAL_SERVICE_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.syncedRolesCount", is(2)));

        // Verify roles are in database
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        assertEquals(2, userRoles.size());
        assertTrue(userRoles.stream().anyMatch(ur -> "Teacher".equals(ur.getRoleName())));
        assertTrue(userRoles.stream().anyMatch(ur -> "CONTENT_MANAGER".equals(ur.getRoleName())));
    }

    @Test
    public void testSyncEiosRolesUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        IntegrationsController.EiosRoleSyncRequest payload = new IntegrationsController.EiosRoleSyncRequest();
        payload.setUserId(userId);
        payload.setRoles(Collections.singletonList("Teacher"));

        // Missing header
        mockMvc.perform(post("/api/v1/integrations/eios/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // Incorrect header
        mockMvc.perform(post("/api/v1/integrations/eios/auth/sync")
                        .header("Authorization", "Bearer WRONG_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testProcessLmsWebhookSuccessAndIndexSearch() throws Exception {
        Map<String, Object> innerPayload = new HashMap<>();
        innerPayload.put("externalId", "tb-101");
        innerPayload.put("externalUrl", "https://teachbase.crie.ru/courses/101");
        innerPayload.put("course_title", "Практический курс эпидемиологии");

        IntegrationsController.LmsWebhookPayload webhookPayload = new IntegrationsController.LmsWebhookPayload();
        webhookPayload.setProvider("Teachbase");
        webhookPayload.setEventType("document.updated");
        webhookPayload.setDocumentId(documentId);
        webhookPayload.setPayload(innerPayload);

        mockMvc.perform(post("/api/v1/integrations/lms/webhooks")
                        .header("Authorization", "Bearer INTERNAL_SERVICE_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", containsString("processed successfully")));

        // Verify metadata created in database
        List<DocumentLmsMetadata> lmsList = documentLmsMetadataRepository.findByDocumentId(documentId);
        assertEquals(1, lmsList.size());
        DocumentLmsMetadata metadata = lmsList.get(0);
        assertEquals("Teachbase", metadata.getLmsProvider());
        assertEquals("tb-101", metadata.getExternalId());
        assertEquals("https://teachbase.crie.ru/courses/101", metadata.getExternalUrl());
        assertTrue(metadata.getMetadataJson().contains("Практический курс эпидемиологии"));

        // Verify search indexes the LMS metadata!
        // Search query "Практический" matches inside metadataJson
        List<DocumentSearchService.SearchResult> searchResults = documentSearchService.search("Практический", null, null);
        assertFalse(searchResults.isEmpty(), "Search should find the document based on LMS metadata content");
        assertEquals(documentId, searchResults.get(0).getDocument().getId());
    }

    @Test
    public void testProcessLmsWebhookUnauthorized() throws Exception {
        IntegrationsController.LmsWebhookPayload webhookPayload = new IntegrationsController.LmsWebhookPayload();
        webhookPayload.setProvider("Moodle");
        webhookPayload.setEventType("document.updated");
        webhookPayload.setDocumentId(documentId);

        mockMvc.perform(post("/api/v1/integrations/lms/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testProcessLmsWebhookDocumentNotFound() throws Exception {
        IntegrationsController.LmsWebhookPayload webhookPayload = new IntegrationsController.LmsWebhookPayload();
        webhookPayload.setProvider("Teachbase");
        webhookPayload.setEventType("document.updated");
        webhookPayload.setDocumentId(UUID.randomUUID()); // Random/not found

        mockMvc.perform(post("/api/v1/integrations/lms/webhooks")
                        .header("Authorization", "Bearer INTERNAL_SERVICE_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }
}
