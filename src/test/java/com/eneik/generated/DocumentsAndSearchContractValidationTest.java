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
public class DocumentsAndSearchContractValidationTest {

    @Test
    public void testDocumentsAndSearchContractExistsAndContainsRequiredFields() throws Exception {
        String path = "docs/contracts/documents-and-search.openapi.yaml";
        File file = new File(path);

        // Assert file exists
        assertTrue(file.exists(), "The OpenAPI contract file must exist at docs/contracts/documents-and-search.openapi.yaml");

        // Read contract content
        String content = Files.readString(Paths.get(path));

        // Verify document search and upload/creation endpoints are defined
        assertTrue(content.contains("/documents:"), "OpenAPI contract must define the documents endpoint");
        assertTrue(content.contains("post:"), "OpenAPI contract must define post method for upload");
        assertTrue(content.contains("/documents/search:"), "OpenAPI contract must define the search endpoint");

        // Verify comment endpoints are defined
        assertTrue(content.contains("/documents/{id}/comments:"), "OpenAPI contract must define comments endpoints");

        // Verify actualization requests are defined
        assertTrue(content.contains("/documents/{id}/actualization-requests:"), "OpenAPI contract must define actualization requests endpoints");

        // Verify key schema types exist
        assertTrue(content.contains("DocumentResponse:"), "OpenAPI contract must define the DocumentResponse schema");
        assertTrue(content.contains("SearchResultResponse:"), "OpenAPI contract must define the SearchResultResponse schema");
        assertTrue(content.contains("CommentResponse:"), "OpenAPI contract must define the CommentResponse schema");
        assertTrue(content.contains("ActualizationRequestResponse:"), "OpenAPI contract must define the ActualizationRequestResponse schema");

        // Verify synonym requirements and Russian abbreviations are documented in the search description
        assertTrue(content.contains("ФГОС"), "OpenAPI contract must document ФГОС synonym mappings");
        assertTrue(content.contains("ГЭК"), "OpenAPI contract must document ГЭК synonym mappings");
        assertTrue(content.contains("ГИА"), "OpenAPI contract must document ГИА synonym mappings");
        assertTrue(content.contains("ФБУН"), "OpenAPI contract must document ФБУН synonym mappings");

        // Verify security schemes and RBAC requirements are documented
        assertTrue(content.contains("BearerAuth:"), "OpenAPI contract must define BearerAuth security scheme");
        assertTrue(content.contains("ForbiddenError:"), "OpenAPI contract must define ForbiddenError response");
        assertTrue(content.contains("UnauthorizedError:"), "OpenAPI contract must define UnauthorizedError response");
        assertTrue(content.contains("Access Control Requirements (RBAC):"), "OpenAPI contract must describe RBAC roles in description");
    }
}
