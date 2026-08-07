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
 * Robust automated test suite verifying Core Platform QA Document Flows and Search workflows.
 * Strictly adheres to BARCAN-TAG-06 Deontic Consistency role principles and GWT specifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QaVerificationDocumentFlowsTest {

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
     * Given the knowledge base API is deployed,
     * When running E2E tests,
     * Then document upload, versioning, and search workflows pass successfully.
     */
    @Test
    public void testDocumentFlowsUploadVersioningAndSearchE2E() throws Exception {
        // 1. Upload a new document as Content Manager (Initial Version 1.0)
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "fgos-epidemiology.pdf", "application/pdf", "ФГОС Эпидемиология контент".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/documents")
                        .file(file1)
                        .param("title", "ФГОС по специальности Эпидемиология")
                        .param("description", "Учебные стандарты для ординаторов")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "residency")
                        .param("process", "certification")
                        .param("documentNumber", "ФГОС-32.08.12-2026")
                        .param("schemaTags", "Load", "Book")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("ФГОС по специальности Эпидемиология")))
                .andExpect(jsonPath("$.version", is("1.0")))
                .andExpect(jsonPath("$.documentType", is("Position")))
                .andExpect(jsonPath("$.academicYear", is("бессрочно")))
                .andExpect(jsonPath("$.program", is("residency")))
                .andExpect(jsonPath("$.process", is("certification")))
                .andExpect(jsonPath("$.documentNumber", is("ФГОС-32.08.12-2026")))
                .andExpect(jsonPath("$.schemaTags", containsInAnyOrder("Load", "Book")))
                .andReturn().getResponse().getContentAsString();

        // Extract ID of the uploaded document from response robustly using JsonPath
        String documentId = JsonPath.read(uploadResponse, "$.id");

        // 2. Search for the document verifying synonym expansion (e.g. searching "федеральный государственный образовательный стандарт" should find "ФГОС")
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "федеральный государственный образовательный стандарт")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(documentId)))
                .andExpect(jsonPath("$[0].document.title", containsString("ФГОС")));

        // 3. Upload a duplicate document with same title to trigger automatic version increment (Version 2.0)
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "fgos-epidemiology-v2.pdf", "application/pdf", "ФГОС Эпидемиология контент новая версия".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file2)
                        .param("title", "ФГОС по специальности Эпидемиология")
                        .param("description", "Обновленные стандарты ординатуры")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "residency")
                        .param("process", "certification")
                        .param("documentNumber", "ФГОС-32.08.12-2026")
                        .header("X-User-Role", "Content Manager"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(documentId)))
                .andExpect(jsonPath("$.version", is("2.0")));

        // 4. Retrieve document details and verify history contains 2 versions
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.id", is(documentId)))
                .andExpect(jsonPath("$.document.version", is("2.0")))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].versionNumber", is(1)))
                .andExpect(jsonPath("$.versions[0].changesSummary", is("Initial upload")))
                .andExpect(jsonPath("$.versions[1].versionNumber", is(2)))
                .andExpect(jsonPath("$.versions[1].changesSummary", is("Uploaded version 2")));
    }

    /**
     * Given the RBAC rules,
     * When running security tests with a student token,
     * Then unauthorized modification endpoints return 403.
     */
    @Test
    public void testStudentTokenCannotModifyDocuments() throws Exception {
        // Attempt upload as Student via X-User-Role header
        MockMultipartFile file = new MockMultipartFile(
                "file", "student-doc.pdf", "application/pdf", "Student upload".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Студенческий документ")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .header("X-User-Role", "Student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("Access forbidden")));

        // Attempt upload as Postgraduate via Authorization Bearer token (Student token)
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Аспирантский документ")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "postgraduate")
                        .param("process", "stipends")
                        .header("Authorization", "Bearer Postgraduate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        // Attempt deletion of an existing document using Student role
        mockMvc.perform(delete("/api/documents/66666666-6666-6666-6666-666666666666")
                        .header("X-User-Role", "Student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }
}
