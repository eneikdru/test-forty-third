package com.eneik.generated;

import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.AnalyticsEvent;
import com.eneik.generated.repository.CategoryRepository;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DocumentSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Autowired
    private com.eneik.generated.util.TimeProvider timeProvider;

    private Category testCategory;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        categoryRepository.deleteAll();
        analyticsEventRepository.deleteAll();
        timeProvider.setFixedDateTime(LocalDateTime.of(2026, 9, 20, 0, 0));

        testCategory = new Category();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Тестовая категория");
        categoryRepository.save(testCategory);
    }

    @AfterEach
    public void teardown() {
        timeProvider.reset();
    }

    private Document createDocument(String title, String description, String program, String docType) {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setCategory(testCategory);
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setProgram(program);
        doc.setDocumentType(docType);
        doc.setStatus("ACTIVE");
        doc.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(doc);
    }

    private void addVersion(Document doc, String changesSummary) {
        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFileUrl("/docs/test.pdf");
        version.setFileType("pdf");
        version.setStatus("ACTIVE");
        version.setAuthorName("Тестовый Автор");
        version.setChangesSummary(changesSummary);
        doc.getVersions().add(version);
        documentRepository.save(doc);
    }

    @Test
    public void testExactSearchInTitleAndDescription() throws Exception {
        Document doc1 = createDocument("Методические материалы по Эпидемиологии", "Правила проведения учебных занятий.", "both", "Other");
        Document doc2 = createDocument("Вопросы к экзамену по Педиатрии", "Список тем для повторения перед ГИА.", "postgraduate", "Position");

        // Search for "Эпидемиологии"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "Эпидемиологии")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(doc1.getId().toString())))
                .andExpect(jsonPath("$[0].document.title", is("Методические материалы по Эпидемиологии")))
                .andExpect(jsonPath("$[0].rank", greaterThan(0.0)));

        // Search for "учебных" (exact in description)
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "учебных")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(doc1.getId().toString())));
    }

    @Test
    public void testBidirectionalSynonymExpansion() throws Exception {
        // Document 1 has abbreviation "ФГОС", should match expanded standard query
        Document doc1 = createDocument("Утвержденный ФГОС по Эпидемиологии", "Описание стандарта обучения.", "both", "Other");

        // Document 2 has expanded text "Федеральный государственный образовательный стандарт", should match abbreviation query "ФГОС"
        Document doc2 = createDocument("Новые стандарты", "Это федеральный государственный образовательный стандарт по педиатрии.", "both", "Position");

        // Query "ФГОС" should return both documents
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФГОС")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc1.getId().toString(), doc2.getId().toString())));

        // Query "федеральный государственный образовательный стандарт" should return both documents
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "федеральный государственный образовательный стандарт")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc1.getId().toString(), doc2.getId().toString())));
    }

    @Test
    public void testFuzzyMatchingWithTypos() throws Exception {
        Document doc = createDocument("Особое уникальное постановление об обучении", "Регламентирует выплаты студентам.", "both", "Position");

        // Query with typo "уникальноо"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Postgraduate")
                        .param("q", "уникальноо")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(doc.getId().toString())));

        // Query with typo "епидемиология" (Russian typo replacing 'э' with 'е')
        Document docEpidem = createDocument("Рабочая программа по Эпидемиологии", "Курс лекций.", "both", "Other");
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Postgraduate")
                        .param("q", "епидемиология")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(docEpidem.getId().toString())));
    }

    @Test
    public void testProgramAndDocumentTypeFilters() throws Exception {
        Document doc1 = createDocument("Регламент ГИА", "Процедура проведения аттестации.", "postgraduate", "Procedure");
        Document doc2 = createDocument("Правила приема", "Порядок поступления.", "residency", "Procedure");
        Document doc3 = createDocument("Спецификация ГИА", "Технические требования.", "both", "Other");

        // Search for "ГИА" filtered by program "postgraduate"
        // doc1 (postgraduate) and doc3 (both) should match, doc2 (residency) is excluded
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ГИА")
                        .param("program", "postgraduate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc1.getId().toString(), doc3.getId().toString())));

        // Search for "ГИА" filtered by documentType "Procedure"
        // Only doc1 (Procedure) should match
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ГИА")
                        .param("documentType", "Procedure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(doc1.getId().toString())));
    }

    @Test
    public void testRbacSecurityChecks() throws Exception {
        // 1. Missing Authorization/X-User-Role should return 401 Unauthorized
        mockMvc.perform(get("/api/documents/search")
                        .param("q", "тест")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        // 2. Invalid role should return 403 Forbidden
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "InvalidRole")
                        .param("q", "тест")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));

        // 3. Valid role (e.g., student) should succeed
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "student")
                        .param("q", "тест")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testNewBidirectionalSynonymExpansion() throws Exception {
        // Document 1 has abbreviation "ФВОКО"
        Document doc1 = createDocument("Документ по ФВОКО", "Общие положения.", "both", "Other");
        // Document 2 has expanded text "Федеральная внутренняя оценка качества образования"
        Document doc2 = createDocument("Регламент оценки", "Федеральная внутренняя оценка качества образования ординаторов.", "both", "Position");

        // Query "ФВОКО" should return both documents
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФВОКО")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc1.getId().toString(), doc2.getId().toString())));

        // Query "федеральная внутренняя оценка качества образования" should return both documents
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "федеральная внутренняя оценка качества образования")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc1.getId().toString(), doc2.getId().toString())));

        // Document 3 has abbreviation "ФОС"
        Document doc3 = createDocument("Инструкция по ФОС", "Для преподавателей.", "both", "Other");
        // Document 4 has expanded text "фонд оценочных средств"
        Document doc4 = createDocument("Оценочные материалы", "Применяется фонд оценочных средств по специальности.", "both", "Position");

        // Query "ФОС" should return both doc3 and doc4
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФОС")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(doc3.getId().toString(), doc4.getId().toString())));
    }

    @Test
    public void testSearchDocumentsPagination() throws Exception {
        // Create 12 documents with "ФГОС" in title to have 12 search results
        for (int i = 1; i <= 12; i++) {
            createDocument("ФГОС Тест " + i, "Описание " + i, "both", "Position");
        }

        // Test 1: Given a search request without pagination parameters, When total results exceed default page size (10),
        // Then it falls back to a default page size and returns a paginated response with totalCount.
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФГОС"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(12)))
                .andExpect(jsonPath("$.data", hasSize(10)));

        // Test 2: Given a search request with page parameters, When the total results exceed the page size,
        // Then the API returns the requested page of documents and the total count.
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФГОС")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(12)))
                .andExpect(jsonPath("$.data", hasSize(5)));

        // Test 3: Given a search request with a specific page number, When that page has no results,
        // Then the API returns an empty array.
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФГОС")
                        .param("page", "5")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Document createFilteredDocument(String title, String description, String program, String docType, String educationLevel, LocalDateTime updatedAt) {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setCategory(testCategory);
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setProgram(program);
        doc.setDocumentType(docType);
        doc.setStatus("ACTIVE");
        doc.setEducationLevel(educationLevel);
        doc.setUpdatedAt(updatedAt);
        return documentRepository.save(doc);
    }

    @Test
    public void testSearchWithEducationLevelAndUpdateDateFilters() throws Exception {
        // Clear setup
        documentRepository.deleteAll();

        // Create 3 documents with different education level and update dates in 2026
        // Anchor reference date is 2026-09-20T00:00:00
        Document docA = createFilteredDocument(
            "Регламент ГИА Аспирантов ЦНИИ", "Процедура проведения аттестации.",
            "postgraduate", "Procedure", "postgraduate_qualification", LocalDateTime.of(2026, 9, 18, 12, 0)
        ); // updated 2 days before anchor (within 7days, 30days, year)

        Document docB = createFilteredDocument(
            "Правила Ординатуры ЦНИИ", "Процедура поступления.",
            "residency", "Procedure", "higher", LocalDateTime.of(2026, 9, 15, 12, 0)
        ); // updated 5 days before anchor (within 7days, 30days, year)

        Document docC = createFilteredDocument(
            "Старые правила ЦНИИ", "Порядок и формы.",
            "both", "Other", "higher", LocalDateTime.of(2026, 1, 10, 10, 0)
        ); // updated in Jan 2026 (within year, NOT within 7days or 30days)

        // 1. Filter by educationLevel "postgraduate_qualification" -> only docA
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ЦНИИ")
                        .param("educationLevel", "postgraduate_qualification")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(docA.getId().toString())));

        // 2. Filter by educationLevel "higher" -> docB and docC
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ЦНИИ")
                        .param("educationLevel", "higher")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(docB.getId().toString(), docC.getId().toString())));

        // 3. Filter by educationLevel "higher" AND updateDate "7days" -> only docB (docC is too old)
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ЦНИИ")
                        .param("educationLevel", "higher")
                        .param("updateDate", "7days")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is(docB.getId().toString())));

        // 4. Filter by updateDate "30days" -> docA and docB
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ЦНИИ")
                        .param("updateDate", "30days")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(docA.getId().toString(), docB.getId().toString())));

        // 5. Filter by updateDate "year" -> docA, docB, docC
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ЦНИИ")
                        .param("updateDate", "year")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder(docA.getId().toString(), docB.getId().toString(), docC.getId().toString())));
    }

    @Test
    public void testSearchLogging() throws Exception {
        analyticsEventRepository.deleteAll();

        String testUserId = "12345678-1234-1234-1234-123456789012";
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "student")
                        .header("X-User-Id", testUserId)
                        .param("q", "Регламент")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        java.util.List<AnalyticsEvent> events = analyticsEventRepository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        AnalyticsEvent event = events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("SEARCH", event.getEventType());
        org.junit.jupiter.api.Assertions.assertEquals("Регламент", event.getSearchQuery());
        org.junit.jupiter.api.Assertions.assertEquals(UUID.fromString(testUserId), event.getUserId());
        org.junit.jupiter.api.Assertions.assertNull(event.getDocumentId());
    }
}
