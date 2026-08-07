package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class ApiContractValidationTest {

    @Test
    public void testOpenApiContractExistsAndContainsRequiredFields() throws Exception {
        String path = "docs/contracts/financial-documents.openapi.yaml";
        File file = new File(path);

        // Assert file exists
        assertTrue(file.exists(), "The OpenAPI contract file must exist at docs/contracts/financial-documents.openapi.yaml");

        // Read contract content
        String content = Files.readString(Paths.get(path));

        // Verify restricted endpoints for stipends and load are defined
        assertTrue(content.contains("/financial/stipends"), "OpenAPI contract must define the stipends endpoint");
        assertTrue(content.contains("/financial/load"), "OpenAPI contract must define the teacher load endpoint");

        // Verify budget cycle metadata fields are documented
        assertTrue(content.contains("BudgetCycleMetadata"), "OpenAPI contract must define the BudgetCycleMetadata schema");
        assertTrue(content.contains("budgetCycle"), "OpenAPI contract must define the budgetCycle field");
        assertTrue(content.contains("estimatedAmount"), "OpenAPI contract must define the estimatedAmount field");
        assertTrue(content.contains("currency"), "OpenAPI contract must define the currency field");
        assertTrue(content.contains("status"), "OpenAPI contract must define the status field");
        assertTrue(content.contains("quarter"), "OpenAPI contract must define the quarter field");
        assertTrue(content.contains("fiscalYear"), "OpenAPI contract must define the fiscalYear field");

        // Verify specialized document metadata fields are documented
        assertTrue(content.contains("DocumentType"), "OpenAPI contract must define the DocumentType schema");
        assertTrue(content.contains("ProgramType"), "OpenAPI contract must define the ProgramType schema");
        assertTrue(content.contains("ProcessType"), "OpenAPI contract must define the ProcessType schema");
        assertTrue(content.contains("academicYear"), "OpenAPI contract must define the academicYear field");
        assertTrue(content.contains("approvalDate"), "OpenAPI contract must define the approvalDate field");
        assertTrue(content.contains("documentNumber"), "OpenAPI contract must define the documentNumber field");
        assertTrue(content.contains("version"), "OpenAPI contract must define the version field");
        assertTrue(content.contains("updatedAt"), "OpenAPI contract must define the updatedAt field");
    }
}
