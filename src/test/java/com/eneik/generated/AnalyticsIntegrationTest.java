package com.eneik.generated;

import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import com.eneik.generated.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Autowired
    private AnalyticsService analyticsService;

    private UUID categoryId;
    private UUID testDocId;

    // Fixed values for deterministic testing
    private final UUID fixedEventId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final LocalDateTime fixedDateTime = LocalDateTime.of(2026, 8, 7, 10, 0, 0);

    @BeforeEach
    public void setUp() {
        // Clear all linked tables
        jdbcTemplate.update("DELETE FROM analytics_events");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");

        // Seed Category
        Category category = new Category();
        categoryId = UUID.randomUUID();
        category.setId(categoryId);
        category.setName("General Analytics Regulations");
        categoryRepository.save(category);

        // Seed Document
        Document doc = new Document();
        testDocId = UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef");
        doc.setId(testDocId);
        doc.setCategory(category);
        doc.setTitle("Standard Operating Procedure for Analytics Export");
        doc.setDescription("Describes methods for EIOS export and log events.");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);

        // Configure deterministic providers
        analyticsService.setUuidProvider(() -> fixedEventId);
        analyticsService.setDateTimeProvider(() -> fixedDateTime);
    }

    @Test
    public void testDownloadDocumentSavesAnalyticsEvent() throws Exception {
        UUID userId = UUID.randomUUID();

        // Perform download
        mockMvc.perform(get("/api/v1/documents/" + testDocId + "/download")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition", containsString("document-" + testDocId + ".txt")));

        // Verify analytics record was saved
        List<AnalyticsEvent> savedEvents = analyticsEventRepository.findAll();
        assertEquals(1, savedEvents.size());

        AnalyticsEvent event = savedEvents.get(0);
        assertEquals(fixedEventId, event.getId());
        assertEquals("DOWNLOAD", event.getEventType());
        assertEquals(userId, event.getUserId());
        assertEquals(testDocId, event.getDocumentId());
        assertEquals(fixedDateTime, event.getCreatedAt());
    }

    @Test
    public void testDownloadDocumentNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/documents/" + randomId + "/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Document not found with ID:")));
    }

    @Test
    public void testAnalyticsExportCsv() throws Exception {
        // Manually save a VIEW event and a DOWNLOAD event
        UUID userId = UUID.randomUUID();
        analyticsService.setUuidProvider(UUID::randomUUID); // use random here to allow multiple records
        analyticsService.logEvent("DOWNLOAD", userId, testDocId, null);
        analyticsService.logEvent("VIEW", userId, testDocId, null);

        // Export as CSV
        mockMvc.perform(get("/api/v1/analytics/export")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-10")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(content().string(containsString("Event ID,Event Type,User ID,Document ID,Document Title,Search Query,Created At")))
                .andExpect(content().string(containsString("DOWNLOAD")))
                .andExpect(content().string(containsString("VIEW")))
                .andExpect(content().string(containsString("Standard Operating Procedure for Analytics Export")));
    }

    @Test
    public void testAnalyticsExportPdf() throws Exception {
        UUID userId = UUID.randomUUID();
        analyticsService.setUuidProvider(() -> fixedEventId);
        analyticsService.logEvent("DOWNLOAD", userId, testDocId, null);

        // Export as PDF
        mockMvc.perform(get("/api/v1/analytics/export")
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("application/pdf")))
                .andExpect(content().string(containsString("%PDF-1.4")))
                .andExpect(content().string(containsString("EIOS Analytics Export Report")));
    }

    @Test
    public void testAnalyticsExportDocx() throws Exception {
        UUID userId = UUID.randomUUID();
        analyticsService.setUuidProvider(() -> fixedEventId);
        analyticsService.logEvent("DOWNLOAD", userId, testDocId, null);

        // Export as DOCX
        mockMvc.perform(get("/api/v1/analytics/export")
                        .param("format", "DOCX"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .andExpect(content().string(containsString("EIOS Analytics Export Report (DOCX Format)")));
    }
}
