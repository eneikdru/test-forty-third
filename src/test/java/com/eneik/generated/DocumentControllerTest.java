package com.eneik.generated;

import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private SchemaTagRepository schemaTagRepository;

    @Autowired
    private UserNotificationPreferenceRepository userNotificationPreferenceRepository;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    @BeforeEach
    public void setUp() {
        // Reset providers
        idProvider.reset();
        timeProvider.reset();

        // Clear tables transactionally
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        userNotificationPreferenceRepository.deleteAll();
        notificationDispatcher.clear();
    }

    @Test
    public void testContentManagerUploadsNewDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "aspirant-stipends.pdf", "application/pdf", "Stipends detail content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Порядок расчета стипендий аспирантов")
                        .param("description", "Регламентирует стипендии")
                        .param("documentType", "Position")
                        .param("academicYear", "2026–2027")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .param("documentNumber", "123-P")
                        .param("schemaTags", "Stipends", "Regulations")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Порядок расчета стипендий аспирантов")))
                .andExpect(jsonPath("$.version", is("1.0")))
                .andExpect(jsonPath("$.documentType", is("Position")))
                .andExpect(jsonPath("$.academicYear", is("2026–2027")))
                .andExpect(jsonPath("$.program", is("postgraduate")))
                .andExpect(jsonPath("$.process", is("stipends")))
                .andExpect(jsonPath("$.documentNumber", is("123-P")))
                .andExpect(jsonPath("$.schemaTags", containsInAnyOrder("Stipends", "Regulations")));
    }

    @Test
    public void testDuplicationControlMergesIntoSameDocumentWithNewVersion() throws Exception {
        // First upload
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "doc1.pdf", "application/pdf", "Content v1".getBytes());

        String response1 = mockMvc.perform(multipart("/api/documents")
                        .file(file1)
                        .param("title", "Порядок расчета стипендий аспирантов")
                        .param("documentType", "Position")
                        .param("academicYear", "2026–2027")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .param("documentNumber", "456-A")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version", is("1.0")))
                .andReturn().getResponse().getContentAsString();

        // Extract ID of created document
        String documentId = response1.substring(response1.indexOf("\"id\":\"") + 6, response1.indexOf("\",\"title\""));

        // Second upload - duplicate title (even if different filename / slightly different casing/whitespace)
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "doc2.pdf", "application/pdf", "Content v2".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file2)
                        .param("title", "  Порядок расчета стипендий аспирантов  ")
                        .param("documentType", "Position")
                        .param("academicYear", "2026–2027")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .param("documentNumber", "456-A")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(documentId)))
                .andExpect(jsonPath("$.version", is("2.0")));

        // Verify version history via GET
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.id", is(documentId)))
                .andExpect(jsonPath("$.document.version", is("2.0")))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].versionNumber", is(1)))
                .andExpect(jsonPath("$.versions[0].changesSummary", is("Initial upload")))
                .andExpect(jsonPath("$.versions[1].versionNumber", is(2)))
                .andExpect(jsonPath("$.versions[1].changesSummary", is("Uploaded version 2")));
    }

    @Test
    public void testContentManagerUploadTriggersNotification() throws Exception {
        // Save user notification preference (active subscriber)
        UUID userId = UUID.randomUUID();
        UserNotificationPreference pref = new UserNotificationPreference(
                UUID.randomUUID(), userId, "tg_chat_id_123", "max_chat_id_123", true
        );
        userNotificationPreferenceRepository.save(pref);

        MockMultipartFile file = new MockMultipartFile(
                "file", "stipends-manual.pdf", "application/pdf", "Stipends manual content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Инструкция по заполнению документов")
                        .param("documentType", "Position")
                        .param("academicYear", "infinite")
                        .param("program", "both")
                        .param("process", "stipends")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated());

        // Verify notification is triggered and received by NotificationDispatcher
        var maxNotifications = notificationDispatcher.getDispatchedMax();
        org.junit.jupiter.api.Assertions.assertEquals(1, maxNotifications.size());
        org.junit.jupiter.api.Assertions.assertEquals("max_chat_id_123", maxNotifications.get(0).getRecipientId());
        org.junit.jupiter.api.Assertions.assertTrue(maxNotifications.get(0).getRenderedMessage().contains("Инструкция по заполнению документов"));
    }

    @Test
    public void testStudentRequestingDeletionReturns403Forbidden() throws Exception {
        UUID docId = UUID.randomUUID();
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("X-User-Role", "Student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("Only Administrators can delete")));
    }

    @Test
    public void testAdministratorRequestingDeletionSucceeds() throws Exception {
        // Upload a document first
        MockMultipartFile file = new MockMultipartFile(
                "file", "aspirant-stipends.pdf", "application/pdf", "Stipends detail content".getBytes());

        String response = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Порядок расчета стипендий аспирантов")
                        .param("documentType", "Position")
                        .param("academicYear", "2026–2027")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String documentId = response.substring(response.indexOf("\"id\":\"") + 6, response.indexOf("\",\"title\""));

        // Delete as Admin
        mockMvc.perform(delete("/api/documents/" + documentId)
                        .header("X-User-Role", "Administrator"))
                .andExpect(status().isNoContent());

        // Verify it was deleted (GET returns 404)
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("X-User-Role", "Administrator"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testInvalidInputValidations() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        // 1. Invalid documentType
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Title")
                        .param("documentType", "InvalidType")
                        .param("academicYear", "infinite")
                        .param("program", "both")
                        .param("process", "other")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Invalid documentType")));

        // 2. Invalid academicYear
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Title")
                        .param("documentType", "Position")
                        .param("academicYear", "202-202")
                        .param("program", "both")
                        .param("process", "other")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Invalid academicYear")));
    }
}
