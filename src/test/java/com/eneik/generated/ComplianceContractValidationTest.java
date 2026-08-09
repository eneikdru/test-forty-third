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
public class ComplianceContractValidationTest {

    @Test
    public void testComplianceContractExistsAndContainsRequiredFields() throws Exception {
        String path = "docs/contracts/compliance-generator.openapi.yaml";
        File file = new File(path);

        // Assert file exists
        assertTrue(file.exists(), "The OpenAPI contract file must exist at docs/contracts/compliance-generator.openapi.yaml");

        // Read contract content
        String content = Files.readString(Paths.get(path));

        // Verify key endpoints are defined
        assertTrue(content.contains("/compliance/coverage-audit:"), "OpenAPI contract must define the coverage-audit endpoint");
        assertTrue(content.contains("/compliance/task-plan:"), "OpenAPI contract must define the task-plan endpoint");

        // Verify security schemes and RBAC requirements are documented
        assertTrue(content.contains("BearerAuth:"), "OpenAPI contract must define BearerAuth security scheme");
        assertTrue(content.contains("ForbiddenError:"), "OpenAPI contract must define ForbiddenError response");
        assertTrue(content.contains("UnauthorizedError:"), "OpenAPI contract must define UnauthorizedError response");
        assertTrue(content.contains("Access Control Requirements (RBAC):"), "OpenAPI contract must describe RBAC roles in description");

        // Verify key schema types exist
        assertTrue(content.contains("Specification:"), "OpenAPI contract must define the Specification schema");
        assertTrue(content.contains("CoverageAudit:"), "OpenAPI contract must define the CoverageAudit schema");
        assertTrue(content.contains("CoverageAuditRequest:"), "OpenAPI contract must define the CoverageAuditRequest schema");
        assertTrue(content.contains("CoverageAuditResponse:"), "OpenAPI contract must define the CoverageAuditResponse schema");
        assertTrue(content.contains("TaskPlan:"), "OpenAPI contract must define the TaskPlan schema");
        assertTrue(content.contains("TaskPlanValidationRequest:"), "OpenAPI contract must define the TaskPlanValidationRequest schema");
        assertTrue(content.contains("TaskPlanValidationResponse:"), "OpenAPI contract must define the TaskPlanValidationResponse schema");
    }
}
