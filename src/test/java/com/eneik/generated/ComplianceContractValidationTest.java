package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class ComplianceContractValidationTest {

    @Test
    public void testComplianceContractExistsAndContainsRequiredFields() throws Exception {
        String path = "docs/contracts/compliance-generator.openapi.yaml";
        File file = new File(path);

        // Assert file exists
        assertTrue(file.exists(), "The compliance generator OpenAPI contract file must exist at docs/contracts/compliance-generator.openapi.yaml");

        // Read contract content
        String content = Files.readString(Paths.get(path));

        // Verify endpoints are defined
        assertTrue(content.contains("/api/v1/compliance/coverage-audit"), "OpenAPI contract must define the coverage-audit endpoint");
        assertTrue(content.contains("/api/v1/compliance/task-plan"), "OpenAPI contract must define the task-plan endpoint");

        // Verify key schemas exist
        assertTrue(content.contains("CoverageAuditRequest"), "OpenAPI contract must define the CoverageAuditRequest schema");
        assertTrue(content.contains("CoverageAuditResponse"), "OpenAPI contract must define the CoverageAuditResponse schema");
        assertTrue(content.contains("TaskPlanRequest"), "OpenAPI contract must define the TaskPlanRequest schema");
        assertTrue(content.contains("TaskPlanResponse"), "OpenAPI contract must define the TaskPlanResponse schema");

        // Verify key fields
        assertTrue(content.contains("gaps"), "OpenAPI contract must define the gaps field");
        assertTrue(content.contains("coverageComplete"), "OpenAPI contract must define the coverageComplete field");
        assertTrue(content.contains("rootCauseRepairs"), "OpenAPI contract must define the rootCauseRepairs field");
    }
}
