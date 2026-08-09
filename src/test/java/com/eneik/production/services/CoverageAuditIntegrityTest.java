package com.eneik.production.services;

import com.eneik.generated.service.TechnicalLeadCompiler;
import com.eneik.generated.service.TechnicalLeadCompiler.CoverageAudit;
import com.eneik.generated.service.TechnicalLeadCompiler.Specification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CoverageAuditIntegrityTest {

    private final TechnicalLeadCompiler compiler = new TechnicalLeadCompiler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCoverageAuditFalsificationDetection() throws IOException {
        // Load the specific coverage audit file referenced in the audit findings
        File file = new File(".eneik/records/coverage-audit-a9e8c79e-d641-4f52-bfda-92a7a6d5cdbe.json");
        assertTrue(file.exists(), "The coverage audit file must exist: " + file.getAbsolutePath());

        // Parse the coverage audit JSON file
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(file, Map.class);
        List<?> gaps = (List<?>) data.get("gaps");
        assertNotNull(gaps, "Coverage audit file must contain a 'gaps' key");

        // The audited file indeed has empty gaps
        assertTrue(gaps.isEmpty(), "Audit file 'coverage-audit-a9e8c79e-d641-4f52-bfda-92a7a6d5cdbe.json' faked success with an empty gaps list");

        // Define the real client specifications for the Knowledge Base
        Specification kbSpec = new Specification(Arrays.asList(
            "document_storage",
            "fulltext_search",
            "user_roles",
            "file_uploads"
        ));

        // Create a CoverageAudit representation from the parsed file
        // Since gaps and addressedSections are empty, it should fail verification against the kbSpec
        CoverageAudit audit = new CoverageAudit(new ArrayList<>(), new ArrayList<>());

        // Verify that trying to validate this audit against the specifications throws IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateCoverageAudit(audit, kbSpec);
        });

        // Verify that the validation message indicates falsified coverage is blocked
        assertTrue(ex.getMessage().contains("Falsified Coverage Audit Blocked"), "Expected falsification block error, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("document_storage") || ex.getMessage().contains("fulltext_search"), "Should list missing sections in exception");
    }
}
