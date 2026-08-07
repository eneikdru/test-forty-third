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
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FinancialDocumentControllerTest {

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
        // Clear transactionally linked tables to guarantee fresh start for each test
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM categories");

        // Create a category
        Category category = new Category();
        categoryId = UUID.randomUUID();
        category.setId(categoryId);
        category.setName("Financial Section");
        categoryRepository.save(category);

        // Fetch the seeded tags
        SchemaTag budgetTag = schemaTagRepository.findByName("Budget").orElseThrow();
        SchemaTag loadTag = schemaTagRepository.findByName("Load").orElseThrow();
        SchemaTag stipendsTag = schemaTagRepository.findByName("Stipends").orElseThrow();

        // Save Budget document
        Document budgetDoc = new Document();
        budgetDocId = UUID.randomUUID();
        budgetDoc.setId(budgetDocId);
        budgetDoc.setCategory(category);
        budgetDoc.setTitle("Положение о бюджете ЦНИИ");
        budgetDoc.setDescription("Определяет бюджетный цикл на текущий год.");
        budgetDoc.setCreatedAt(LocalDateTime.now());
        budgetDoc.setUpdatedAt(LocalDateTime.now());
        budgetDoc.getSchemaTags().add(budgetTag);
        budgetDoc.setDocumentType("Position");
        budgetDoc.setAcademicYear("2026–2027");
        budgetDoc.setProgram("both");
        budgetDoc.setProcess("other");
        documentRepository.save(budgetDoc);

        // Save Load document
        Document loadDoc = new Document();
        loadDocId = UUID.randomUUID();
        loadDoc.setId(loadDocId);
        loadDoc.setCategory(category);
        loadDoc.setTitle("Порядок расчета учебной нагрузки преподавателей");
        loadDoc.setDescription("Формулы расчета и распределения нагрузки.");
        loadDoc.setCreatedAt(LocalDateTime.now());
        loadDoc.setUpdatedAt(LocalDateTime.now());
        loadDoc.getSchemaTags().add(loadTag);
        loadDoc.setDocumentType("Procedure");
        loadDoc.setAcademicYear("2026–2027");
        loadDoc.setProgram("both");
        loadDoc.setProcess("other");
        documentRepository.save(loadDoc);

        // Save Stipends document
        Document stipendDoc = new Document();
        stipendDocId = UUID.randomUUID();
        stipendDoc.setId(stipendDocId);
        stipendDoc.setCategory(category);
        stipendDoc.setTitle("Положение о стипендиях аспирантов");
        stipendDoc.setDescription("Регламентирует выплаты стипендий.");
        stipendDoc.setCreatedAt(LocalDateTime.now());
        stipendDoc.setUpdatedAt(LocalDateTime.now());
        stipendDoc.getSchemaTags().add(stipendsTag);
        stipendDoc.setDocumentType("Position");
        stipendDoc.setAcademicYear("2026–2027");
        stipendDoc.setProgram("postgraduate");
        stipendDoc.setProcess("stipends");
        documentRepository.save(stipendDoc);
    }

    @Test
    public void testEconomistGetsBudgetReport() throws Exception {
        mockMvc.perform(get("/api/financial/budget")
                        .header("X-User-Role", "Economist")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(budgetDocId.toString())))
                .andExpect(jsonPath("$[0].title", is("Положение о бюджете ЦНИИ")))
                .andExpect(jsonPath("$[0].documentType", is("Position")))
                .andExpect(jsonPath("$[0].academicYear", is("2026–2027")))
                .andExpect(jsonPath("$[0].program", is("both")))
                .andExpect(jsonPath("$[0].process", is("other")))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.budgetCycle", is("2026 Budget Cycle")))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.estimatedAmount", is(1500000.00)))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.currency", is("RUB")))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.status", is("APPROVED")))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.quarter", is("Q1")))
                .andExpect(jsonPath("$[0].budgetCycleMetadata.fiscalYear", is(2026)));
    }

    @Test
    public void testTeacherGetsForbiddenForBudgetReport() throws Exception {
        mockMvc.perform(get("/api/financial/budget")
                        .header("X-User-Role", "Teacher")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Budget'")));
    }

    @Test
    public void testTeacherGetsLoadDocuments() throws Exception {
        mockMvc.perform(get("/api/financial/load")
                        .header("X-User-Role", "Teacher")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(loadDocId.toString())))
                .andExpect(jsonPath("$[0].title", is("Порядок расчета учебной нагрузки преподавателей")))
                .andExpect(jsonPath("$[0].documentType", is("Procedure")))
                .andExpect(jsonPath("$[0].schemaTags", contains("Load")));
    }

    @Test
    public void testPostgraduateGetsForbiddenForLoad() throws Exception {
        mockMvc.perform(get("/api/financial/load")
                        .header("X-User-Role", "Postgraduate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Load'")));
    }

    @Test
    public void testPostgraduateGetsStipends() throws Exception {
        mockMvc.perform(get("/api/financial/stipends")
                        .header("Authorization", "Bearer Postgraduate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(stipendDocId.toString())))
                .andExpect(jsonPath("$[0].title", is("Положение о стипендиях аспирантов")))
                .andExpect(jsonPath("$[0].documentType", is("Position")))
                .andExpect(jsonPath("$[0].program", is("postgraduate")))
                .andExpect(jsonPath("$[0].process", is("stipends")))
                .andExpect(jsonPath("$[0].schemaTags", contains("Stipends")));
    }

    @Test
    public void testMissingRoleGetsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/financial/budget")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Missing or invalid credentials")));
    }
}
