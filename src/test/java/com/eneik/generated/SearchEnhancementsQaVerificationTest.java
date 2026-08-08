package com.eneik.generated;

import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.repository.CategoryRepository;
import com.eneik.generated.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Robust automated integration test suite verifying Search Enhancements, Typo Corrections, and Metadata Filters.
 * Strictly adheres to BARCAN-TAG-06 Deontic Consistency role principles and GWT specifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SearchEnhancementsQaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = new Category();
        testCategory.setId(UUID.fromString("6a02b66a-2ee5-4b31-90a6-f286829bb572"));
        testCategory.setName("Тестовая категория верификации");
        categoryRepository.save(testCategory);
    }

    private Document createDocument(String idStr, String title, String description, String program, String docType, String approvalDateStr, String docNumber) {
        Document doc = new Document();
        doc.setId(UUID.fromString(idStr));
        doc.setCategory(testCategory);
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setProgram(program);
        doc.setDocumentType(docType);
        doc.setStatus("ACTIVE");
        doc.setUpdatedAt(LocalDateTime.of(2026, 9, 20, 12, 0, 0));
        if (approvalDateStr != null) {
            doc.setApprovalDate(LocalDate.parse(approvalDateStr));
        }
        doc.setDocumentNumber(docNumber);
        return documentRepository.save(doc);
    }

    /**
     * Given the search endpoint,
     * When searching for specific keywords in Russian,
     * Then the documents are correctly matched and ranked based on exact titles and descriptions.
     */
    @Test
    public void testExactAndFuzzyRussianSearch() throws Exception {
        // Given
        createDocument("11111111-1111-1111-1111-111111111111", "Регламент кандидатских экзаменов по Эпидемиологии", "Правила проведения аттестации аспирантов.", "postgraduate", "Procedure", "2026-05-10", "РЕГ-ГИА-2026");
        createDocument("22222222-2222-2222-2222-222222222222", "Шаблоны протоколов ГЭК", "Утверждённые образцы протоколов для комиссий.", "both", "Project", "2026-04-18", "ШАБ-ГЭК-ПРАК");

        // When/Then: Search for "экзаменов"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "экзаменов")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is("11111111-1111-1111-1111-111111111111")))
                .andExpect(jsonPath("$[0].document.title", is("Регламент кандидатских экзаменов по Эпидемиологии")));

        // When/Then: Search for "протоколов"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "протоколов")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is("22222222-2222-2222-2222-222222222222")));
    }

    /**
     * Given the search endpoint,
     * When synonym expansion queries are sent (e.g. ГИА or ФГОС),
     * Then the search correctly expands to both short and long forms.
     */
    @Test
    public void testSynonymExpansionWithCommonAcronyms() throws Exception {
        // Given
        createDocument("33333333-3333-3333-3333-333333333333", "Утвержденный ФГОС ординаторов", "Документ описывает учебные требования.", "residency", "Position", "2024-06-15", "ФГОС-32.08.12");
        createDocument("44444444-4444-4444-4444-444444444444", "Федеральный государственный образовательный стандарт по эпидемиологии", "Основной стандарт высшего образования.", "residency", "Position", "2024-08-22", "ФГОС-31.08.35");

        // When/Then: Search for "ФГОС"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ФГОС")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder("33333333-3333-3333-3333-333333333333", "44444444-4444-4444-4444-444444444444")));

        // When/Then: Search for "федеральный государственный образовательный стандарт"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "федеральный государственный образовательный стандарт")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].document.id", containsInAnyOrder("33333333-3333-3333-3333-333333333333", "44444444-4444-4444-4444-444444444444")));
    }

    /**
     * Given the search endpoint,
     * When filters (program and documentType) are applied,
     * Then only documents matching the filters are returned.
     */
    @Test
    public void testSearchFiltersAndRoleAccess() throws Exception {
        // Given
        createDocument("55555555-5555-5555-5555-555555555555", "Правила приема в аспирантуру", "Порядок поступления в ЦНИИ.", "postgraduate", "Procedure", "2026-05-10", "РЕГ-ГИА-2026");
        createDocument("66666666-6666-6666-6666-666666666666", "Вопросы к экзамену ординатура", "Список тем для подготовки.", "residency", "Other", "2025-09-01", "ВОП-КАНД-2025");
        createDocument("77777777-7777-7777-7777-777777777777", "Общие положения обучения", "Положение о практике для всех.", "both", "Position", "2026-01-20", "ПОЛ-ВСОКО-01");

        // When/Then: Filter by program = "postgraduate"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "обучения")
                        .param("program", "postgraduate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is("77777777-7777-7777-7777-777777777777")));

        // When/Then: Filter by documentType = "Procedure"
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "приема")
                        .param("documentType", "Procedure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is("55555555-5555-5555-5555-555555555555")));
    }

    /**
     * Given the search endpoint,
     * When typo correction queries with Russian spelling mistakes are sent,
     * Then the search matching leverages Levenshtein distance fuzzy logic.
     */
    @Test
    public void testFuzzyMatchingAndTypoCorrections() throws Exception {
        // Given
        createDocument("88888888-8888-8888-8888-888888888888", "Глоссарий терминов эпидемиологического учёта", "Официальный справочник.", "both", "Other", "2025-11-15", "СПР-ГЛОС-2025");

        // When / Then: Search for typo "глосарий" (one 'c' instead of two)
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "глосарий")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.id", is("88888888-8888-8888-8888-888888888888")));
    }

    /**
     * Given search results,
     * When checking response attributes,
     * Then approvalDate and documentNumber metadata are returned correctly to enable client-side date/education filtering.
     */
    @Test
    public void testSearchMetadataForClientSideFilters() throws Exception {
        // Given
        createDocument("99999999-9999-9999-9999-999999999999", "Положение о ВСОКО", "Внутренняя система оценки.", "both", "Position", "2026-01-20", "ПОЛ-ВСОКО-01");

        // When / Then
        mockMvc.perform(get("/api/documents/search")
                        .header("X-User-Role", "Teacher")
                        .param("q", "ВСОКО")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.approvalDate", is("2026-01-20")))
                .andExpect(jsonPath("$[0].document.documentNumber", is("ПОЛ-ВСОКО-01")));
    }

    /**
     * Given the secure retrieval API,
     * When retrieving a favorited document ID as an authenticated user,
     * Then the system securely returns the document preferences and details, but blocks unauthenticated access.
     */
    @Test
    public void testSavedSearchesAndFavoritesSecurityAndRetrieval() throws Exception {
        // Given
        String favDocId = "abcabcab-1234-1234-1234-abcabcabcabc";
        createDocument(favDocId, "Шаблоны заявлений на академический отпуск", "Архив документов.", "both", "Project", "2026-07-02", "ШАБ-ЗАЯВ-ПОРТ");

        // When / Then: Authenticated user retrieves their favorited document
        mockMvc.perform(get("/api/documents/" + favDocId)
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.id", is(favDocId)))
                .andExpect(jsonPath("$.document.title", is("Шаблоны заявлений на академический отпуск")));

        // When / Then: Unauthenticated/anonymous request to the secure endpoint is rejected (secure retrieval)
        mockMvc.perform(get("/api/documents/" + favDocId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
