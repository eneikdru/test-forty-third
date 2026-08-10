package com.eneik.generated;

import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.DocumentVersionRepository;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FilePersistenceAndRetrievalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    private static final Path UPLOADS_DIR = Paths.get("data", "uploads");

    @BeforeEach
    public void setUp() {
        idProvider.reset();
        timeProvider.reset();

        jdbcTemplate.update("DELETE FROM analytics_events");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up uploads directory to keep tests pristine
        if (Files.exists(UPLOADS_DIR)) {
            Files.walk(UPLOADS_DIR)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Suppress
                        }
                    });
        }
    }

    @Test
    public void testUploadAndDownloadFileSuccessfully() throws Exception {
        byte[] originalContent = "This is some dummy PDF content representing a policy guideline.".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "guideline.pdf", "application/pdf", originalContent);

        // 1. Upload the file
        String response = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Руководство по дезинфекции")
                        .param("description", "Правила дезинфекции помещений")
                        .param("documentType", "Procedure")
                        .param("academicYear", "2026-2027")
                        .param("program", "both")
                        .param("process", "practice")
                        .param("documentNumber", "PROC-999")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Extract ID of the newly created document
        String documentId = response.substring(response.indexOf("\"id\":\"") + 6, response.indexOf("\",\"title\""));

        // 2. Verify that the file exists on disk
        Path expectedFilePath = UPLOADS_DIR.resolve(Paths.get(documentId, "v1", "guideline.pdf"));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(expectedFilePath), "The file should have been stored on disk");
        org.junit.jupiter.api.Assertions.assertArrayEquals(originalContent, Files.readAllBytes(expectedFilePath), "Saved file content must match original");

        // 3. Make GET request to the generated file URL and verify retrieval
        String fileUrl = "/api/files/" + documentId + "/v1/guideline.pdf";
        mockMvc.perform(get(fileUrl)
                        .header("X-User-Role", "Student"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"guideline.pdf\""))
                .andExpect(content().bytes(originalContent));
    }

    @Test
    public void testDownloadFileNotFoundReturns404() throws Exception {
        String nonexistentFileUrl = "/api/files/" + UUID.randomUUID() + "/v1/missing.pdf";
        mockMvc.perform(get(nonexistentFileUrl)
                        .header("X-User-Role", "Student"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("File not found")));
    }

    @Test
    public void testDownloadFileUnauthorizedReturns401() throws Exception {
        String nonexistentFileUrl = "/api/files/" + UUID.randomUUID() + "/v1/missing.pdf";
        mockMvc.perform(get(nonexistentFileUrl))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    public void testDownloadRestrictedFileAccessControl() throws Exception {
        byte[] originalContent = "Confidential Budget Data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "budget_data.pdf", "application/pdf", originalContent);

        // 1. Upload the file with "Budget" schema tag
        String response = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Бюджетный отчет")
                        .param("description", "Детализированный отчет по бюджету")
                        .param("documentType", "Procedure")
                        .param("academicYear", "2026-2027")
                        .param("program", "both")
                        .param("process", "practice")
                        .param("documentNumber", "PROC-888")
                        .param("schemaTags", "Budget")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Extract ID of the newly created document
        String documentId = response.substring(response.indexOf("\"id\":\"") + 6, response.indexOf("\",\"title\""));

        // 2. Try downloading as "Teacher" (Teacher role is not assigned to Budget schema tag) -> should return 403 Forbidden
        String fileUrl = "/api/files/" + documentId + "/v1/budget_data.pdf";
        mockMvc.perform(get(fileUrl)
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        // 3. Try downloading as "Economist" (Economist role is assigned to Budget schema tag) -> should return 200 OK
        mockMvc.perform(get(fileUrl)
                        .header("X-User-Role", "Economist"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(originalContent));
    }
}
