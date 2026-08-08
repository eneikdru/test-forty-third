package com.eneik.generated;

import com.eneik.generated.model.Document;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentFeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    private UUID existingDocId;

    @BeforeEach
    public void setUp() {
        idProvider.reset();
        timeProvider.reset();

        jdbcTemplate.update("DELETE FROM document_comments");
        jdbcTemplate.update("DELETE FROM document_actualization_requests");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");

        // Create a dummy document for reference
        Document doc = new Document();
        existingDocId = UUID.randomUUID();
        doc.setId(existingDocId);
        doc.setTitle("Инструкция по обратной связи");
        doc.setDocumentType("Other");
        doc.setAcademicYear("infinite");
        doc.setProgram("both");
        doc.setProcess("other");
        doc.setStatus("ACTIVE");
        doc.setCreatedAt(timeProvider.now());
        doc.setUpdatedAt(timeProvider.now());

        documentRepository.save(doc);
    }

    @Test
    public void testPostAndGetCommentSucceeds() throws Exception {
        String commentJson = "{\"text\": \"Пожалуйста, уточните второй пункт регламента.\"}";

        // POST a comment
        mockMvc.perform(post("/api/documents/" + existingDocId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Role", "Teacher")
                        .header("X-User-Id", "00000000-0000-0000-0000-000000000111"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is("00000000-0000-0000-0000-000000000111")))
                .andExpect(jsonPath("$.userName", is("Преподаватель")))
                .andExpect(jsonPath("$.text", is("Пожалуйста, уточните второй пункт регламента.")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        // GET comments
        mockMvc.perform(get("/api/documents/" + existingDocId + "/comments")
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text", is("Пожалуйста, уточните второй пункт регламента.")))
                .andExpect(jsonPath("$[0].userName", is("Преподаватель")));
    }

    @Test
    public void testPostCommentValidationAndNotFound() throws Exception {
        // 1. Missing header
        mockMvc.perform(post("/api/documents/" + existingDocId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"hello\"}"))
                .andExpect(status().isUnauthorized());

        // 2. Empty text (400 Bad Request)
        mockMvc.perform(post("/api/documents/" + existingDocId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}")
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));

        // 3. Document not found (404)
        mockMvc.perform(post("/api/documents/" + UUID.randomUUID() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"valid content\"}")
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    public void testPostActualizationRequestSucceeds() throws Exception {
        String requestJson = "{\"reason\": \"Стандарты ФГОС изменились в соответствии с новым приказом.\"}";

        mockMvc.perform(post("/api/documents/" + existingDocId + "/actualization-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("X-User-Role", "Teacher")
                        .header("X-User-Id", "00000000-0000-0000-0000-000000000222"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.documentId", is(existingDocId.toString())))
                .andExpect(jsonPath("$.requesterId", is("00000000-0000-0000-0000-000000000222")))
                .andExpect(jsonPath("$.reason", is("Стандарты ФГОС изменились в соответствии с новым приказом.")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }
}
