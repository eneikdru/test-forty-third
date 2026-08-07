package com.eneik.generated;

import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QA Access Control Security regression tests.
 * Focuses on verifying negative access scenarios as per BARCAN-TAG-06 / QA Verification role.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FinancialAccessControlQaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SchemaTagRepository schemaTagRepository;

    private UUID categoryId;
    private UUID budgetDocId;
    private UUID loadDocId;
    private UUID stipendDocId;

    @BeforeEach
    public void setUp() {
        // Clean database tables to avoid pollution
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");

        // Seed Category
        Category category = new Category();
        categoryId = UUID.randomUUID();
        category.setId(categoryId);
        category.setName("Financial QA Testing Section");
        categoryRepository.save(category);

        // Fetch seeded SchemaTags
        SchemaTag budgetTag = schemaTagRepository.findByName("Budget").orElseThrow();
        SchemaTag loadTag = schemaTagRepository.findByName("Load").orElseThrow();
        SchemaTag stipendsTag = schemaTagRepository.findByName("Stipends").orElseThrow();

        // Save Budget document
        Document budgetDoc = new Document();
        budgetDocId = UUID.randomUUID();
        budgetDoc.setId(budgetDocId);
        budgetDoc.setCategory(category);
        budgetDoc.setTitle("Положение о бюджете ЦНИИ - QA TEST");
        budgetDoc.setDescription("Определяет бюджетный цикл на текущий год.");
        budgetDoc.setCreatedAt(LocalDateTime.now());
        budgetDoc.setUpdatedAt(LocalDateTime.now());
        budgetDoc.getSchemaTags().add(budgetTag);
        documentRepository.save(budgetDoc);

        // Save Load document
        Document loadDoc = new Document();
        loadDocId = UUID.randomUUID();
        loadDoc.setId(loadDocId);
        loadDoc.setCategory(category);
        loadDoc.setTitle("Порядок расчета учебной нагрузки преподавателей - QA TEST");
        loadDoc.setDescription("Формулы расчета и распределения нагрузки.");
        loadDoc.setCreatedAt(LocalDateTime.now());
        loadDoc.setUpdatedAt(LocalDateTime.now());
        loadDoc.getSchemaTags().add(loadTag);
        documentRepository.save(loadDoc);

        // Save Stipends document
        Document stipendDoc = new Document();
        stipendDocId = UUID.randomUUID();
        stipendDoc.setId(stipendDocId);
        stipendDoc.setCategory(category);
        stipendDoc.setTitle("Положение о стипендиях аспирантов - QA TEST");
        stipendDoc.setDescription("Регламентирует выплаты стипендий.");
        stipendDoc.setCreatedAt(LocalDateTime.now());
        stipendDoc.setUpdatedAt(LocalDateTime.now());
        stipendDoc.getSchemaTags().add(stipendsTag);
        documentRepository.save(stipendDoc);
    }

    /**
     * AC 1: Given the financial module is deployed,
     * When running security regression tests,
     * Then Teachers cannot access budget templates.
     */
    @Test
    public void testTeacherCannotAccessBudgetTemplates() throws Exception {
        mockMvc.perform(get("/api/financial/budget")
                        .header("X-User-Role", "Teacher")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Budget'")));
    }

    /**
     * Extra security regression verification:
     * Postgraduates/students cannot access budget templates.
     */
    @Test
    public void testPostgraduateCannotAccessBudgetTemplates() throws Exception {
        mockMvc.perform(get("/api/financial/budget")
                        .header("X-User-Role", "Postgraduate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Budget'")));
    }

    /**
     * Extra security regression verification:
     * Postgraduates/students cannot access load calculations.
     */
    @Test
    public void testPostgraduateCannotAccessLoadTemplates() throws Exception {
        mockMvc.perform(get("/api/financial/load")
                        .header("X-User-Role", "Postgraduate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Load'")));
    }

    /**
     * Positive security verification:
     * Teachers can access load calculations.
     */
    @Test
    public void testTeacherCanAccessLoadTemplates() throws Exception {
        mockMvc.perform(get("/api/financial/load")
                        .header("X-User-Role", "Teacher")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Порядок расчета учебной нагрузки")));
    }

    /**
     * Positive security verification:
     * Postgraduates can access stipends.
     */
    @Test
    public void testPostgraduateCanAccessStipends() throws Exception {
        mockMvc.perform(get("/api/financial/stipends")
                        .header("X-User-Role", "Postgraduate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Положение о стипендиях")));
    }
}
