package com.eneik.generated;

import com.eneik.generated.repository.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Robust automated test suite verifying student status document flows (transfer, dismissal, reinstatement, leaves) and search workflows.
 * Strictly adheres to BARCAN-TAG-06 Deontic Consistency role principles and GWT specifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StudentStatusDocumentFlowsTest {

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

    @BeforeEach
    public void setUp() {
        // Reset providers for complete reproducibility
        idProvider.reset();
        timeProvider.reset();

        // Clear tables transactionally to ensure isolated execution (Popper's critical rationalist isolation)
        jdbcTemplate.update("DELETE FROM analytics_events");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
    }

    /**
     * Given the student status document API is deployed,
     * When running E2E tests for student transfer, dismissal, reinstatement, and leaves,
     * Then document upload, versioning, and search workflows pass successfully.
     */
    @Test
    public void testStudentStatusDocumentsE2EFlow() throws Exception {
        // 1. Upload a Transfer Document (Положение о переводе)
        MockMultipartFile fileTransfer = new MockMultipartFile(
                "file", "transfer-policy.pdf", "application/pdf", "Правила перевода студентов".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/documents")
                        .file(fileTransfer)
                        .param("title", "Положение о переводе студентов ЦНИИ")
                        .param("description", "Регламент перевода, отчисления и восстановления")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "both")
                        .param("process", "certification")
                        .param("documentNumber", "ПЕРЕВОД-2026-01")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Положение о переводе студентов ЦНИИ")))
                .andExpect(jsonPath("$.version", is("1.0")))
                .andReturn().getResponse().getContentAsString();

        String documentId = JsonPath.read(uploadResponse, "$.id");

        // 2. Upload a Leave Document (Положение об отпусках)
        MockMultipartFile fileLeave = new MockMultipartFile(
                "file", "leave-policy.pdf", "application/pdf", "Порядок предоставления академических отпусков".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(fileLeave)
                        .param("title", "Порядок предоставления отпусков ординаторам")
                        .param("description", "Правила предоставления академических отпусков")
                        .param("documentType", "Procedure")
                        .param("academicYear", "бессрочно")
                        .param("program", "residency")
                        .param("process", "other")
                        .param("documentNumber", "ОТПУСК-2026")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Порядок предоставления отпусков ординаторам")));

        // 3. Search for student status documents by query
        // Searching for "перевод" should find the first document
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "перевод")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].document.id", is(documentId)))
                .andExpect(jsonPath("$[0].document.title", containsString("переводе")));

        // Searching for "отпуск" should find the second document as the highest rank
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "отпуск")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].document.title", containsString("отпусков")));

        // 4. Update the transfer document with a new version (Version 2.0)
        MockMultipartFile fileTransferV2 = new MockMultipartFile(
                "file", "transfer-policy-v2.pdf", "application/pdf", "Правила перевода студентов версия 2".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(fileTransferV2)
                        .param("title", "Положение о переводе студентов ЦНИИ")
                        .param("description", "Регламент перевода, отчисления и восстановления - новая редакция")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "both")
                        .param("process", "certification")
                        .param("documentNumber", "ПЕРЕВОД-2026-01")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(documentId)))
                .andExpect(jsonPath("$.version", is("2.0")));

        // 5. Verify the document history has both versions
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.id", is(documentId)))
                .andExpect(jsonPath("$.document.version", is("2.0")))
                .andExpect(jsonPath("$.versions", hasSize(2)));
    }

    /**
     * Given the RBAC rules for student status documents,
     * When running security tests with a student token,
     * Then unauthorized modification endpoints return 403.
     */
    @Test
    public void testStudentTokenCannotModifyStatusDocuments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious-transfer.pdf", "application/pdf", "Fake transfer rule".getBytes());

        // Student tries to upload a new transfer rule
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Положение о переводе без экзаменов")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "both")
                        .param("process", "certification")
                        .header("X-User-Role", "Student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        // Student tries to delete a document
        mockMvc.perform(delete("/api/documents/12345678-1234-1234-1234-123456789012")
                        .header("X-User-Role", "Student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }
}
