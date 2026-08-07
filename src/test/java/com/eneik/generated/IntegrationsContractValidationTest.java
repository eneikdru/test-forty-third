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

        // Verify analytics export endpoint
        assertTrue(content.contains("/api/v1/analytics/export:"), "OpenAPI contract must define the analytics export endpoint");

        // Verify LMS webhooks and EIOS auth sync endpoints
        assertTrue(content.contains("/api/v1/integrations/lms/webhooks:"), "OpenAPI contract must define the LMS webhooks endpoint");
        assertTrue(content.contains("/api/v1/integrations/eios/auth/sync:"), "OpenAPI contract must define the EIOS auth sync endpoint");

        // Verify schemas
        assertTrue(content.contains("LmsWebhookPayload:"), "OpenAPI contract must define the LmsWebhookPayload schema");
        assertTrue(content.contains("EiosRoleSyncRequest:"), "OpenAPI contract must define the EiosRoleSyncRequest schema");
    }
}
