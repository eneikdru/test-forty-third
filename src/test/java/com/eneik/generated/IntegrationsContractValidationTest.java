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
public class IntegrationsContractValidationTest {

    @Test
    public void testIntegrationsContractExistsAndContainsRequiredFields() throws Exception {
        String path = "docs/contracts/Integrations.openapi.yaml";
        File file = new File(path);

        // Assert file exists
        assertTrue(file.exists(), "The OpenAPI contract file must exist at docs/contracts/Integrations.openapi.yaml");

        // Read contract content
        String content = Files.readString(Paths.get(path));

        // Verify EIOS auth sync, LMS webhooks, and analytics export endpoints are defined
        assertTrue(content.contains("/api/v1/integrations/lms/webhooks"), "OpenAPI contract must define the LMS webhook callbacks endpoint");
        assertTrue(content.contains("/api/v1/analytics/export"), "OpenAPI contract must define the analytics export endpoint");
        assertTrue(content.contains("/api/v1/integrations/eios/auth/sync"), "OpenAPI contract must define the EIOS roles/auth sync endpoint");

        // Verify key schemas exist
        assertTrue(content.contains("LmsWebhookPayload"), "OpenAPI contract must define the LmsWebhookPayload schema");
        assertTrue(content.contains("EiosRoleSyncRequest"), "OpenAPI contract must define the EiosRoleSyncRequest schema");

        // Verify query parameters for analytics export exist
        assertTrue(content.contains("startDate"), "OpenAPI contract must define startDate parameter for analytics export");
        assertTrue(content.contains("endDate"), "OpenAPI contract must define endDate parameter for analytics export");
        assertTrue(content.contains("format"), "OpenAPI contract must define format parameter for analytics export");

        // Verify format options are documented
        assertTrue(content.contains("PDF"), "OpenAPI contract must document PDF format");
        assertTrue(content.contains("DOCX"), "OpenAPI contract must document DOCX format");
        assertTrue(content.contains("CSV"), "OpenAPI contract must document CSV format");
    }
}
