package com.eneik.generated;

import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
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

    private Category testCategory;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = new Category();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Тестовая категория");
        categoryRepository.save(testCategory);
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
}
