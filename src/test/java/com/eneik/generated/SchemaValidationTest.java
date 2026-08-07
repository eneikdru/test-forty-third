package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class SchemaValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSchemaExists() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList("SHOW TABLES");
        String tablesStr = tables.toString().toLowerCase();

        assertTrue(tablesStr.contains("roles"));
        assertTrue(tablesStr.contains("categories"));
        assertTrue(tablesStr.contains("documents"));
        assertTrue(tablesStr.contains("document_versions"));
    }

    @Test
    public void testDocumentVersionsQuery() {
        UUID docId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        String catName = "Test Category " + catId.toString();

        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)", catId, catName);
        jdbcTemplate.update("INSERT INTO documents (id, category_id, title) VALUES (?, ?, ?)", docId, catId, "Test Doc");

        jdbcTemplate.update("INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), docId, 1, "url1", "PDF", "ARCHIVED", "Author 1");

        jdbcTemplate.update("INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), docId, 2, "url2", "PDF", "ACTIVE", "Author 1");

        List<Map<String, Object>> activeVersions = jdbcTemplate.queryForList(
                "SELECT * FROM document_versions WHERE document_id = ? AND status = 'ACTIVE'", docId);

        assertEquals(1, activeVersions.size());
        assertEquals(2, activeVersions.get(0).get("version_number"));

        List<Map<String, Object>> archivedVersions = jdbcTemplate.queryForList(
                "SELECT * FROM document_versions WHERE document_id = ? AND status = 'ARCHIVED'", docId);

        assertEquals(1, archivedVersions.size());
        assertEquals(1, archivedVersions.get(0).get("version_number"));
    }

    @Test
    public void testFinancialMetadataAndHRRolesSeeded() {
        // Verify new tables exist
        List<Map<String, Object>> tables = jdbcTemplate.queryForList("SHOW TABLES");
        String tablesStr = tables.toString().toLowerCase();
        assertTrue(tablesStr.contains("schema_tags"));
        assertTrue(tablesStr.contains("document_schema_tags"));
        assertTrue(tablesStr.contains("role_schema_tags"));

        // Verify "Budget" and "Load" schema tags are seeded
        List<Map<String, Object>> tags = jdbcTemplate.queryForList("SELECT * FROM schema_tags WHERE name IN ('Budget', 'Load') ORDER BY name");
        assertEquals(2, tags.size());
        assertEquals("Budget", tags.get(0).get("name"));
        assertEquals("Load", tags.get(1).get("name"));

        // Verify "Economist" and "HR" roles are seeded
        List<Map<String, Object>> hrRoles = jdbcTemplate.queryForList("SELECT * FROM roles WHERE name IN ('Economist', 'HR') ORDER BY name");
        assertEquals(2, hrRoles.size());
        assertEquals("Economist", hrRoles.get(0).get("name"));
        assertEquals("HR", hrRoles.get(1).get("name"));
    }

    @Test
    public void testAssociationTablesConstraints() {
        // Insert category & document to test document_schema_tags
        UUID docId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)", catId, "Category " + catId);
        jdbcTemplate.update("INSERT INTO documents (id, category_id, title) VALUES (?, ?, ?)", docId, catId, "Financial Report");

        // Fetch a tag id
        UUID tagId = UUID.fromString(jdbcTemplate.queryForObject("SELECT id FROM schema_tags WHERE name = 'Budget'", String.class));

        // Link document and schema tag
        jdbcTemplate.update("INSERT INTO document_schema_tags (document_id, schema_tag_id) VALUES (?, ?)", docId, tagId);

        // Fetch link
        List<Map<String, Object>> docLinks = jdbcTemplate.queryForList(
                "SELECT * FROM document_schema_tags WHERE document_id = ? AND schema_tag_id = ?", docId, tagId);
        assertEquals(1, docLinks.size());

        // Create a dedicated role and tag to avoid unique constraint collisions with pre-seeded data
        UUID testRoleId = UUID.randomUUID();
        UUID testTagId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO roles (id, name, description) VALUES (?, ?, ?)", testRoleId, "TestRole_" + testRoleId, "Test Description");
        jdbcTemplate.update("INSERT INTO schema_tags (id, name, description) VALUES (?, ?, ?)", testTagId, "TestTag_" + testTagId, "Test Description");

        // Link role and schema tag
        jdbcTemplate.update("INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES (?, ?)", testRoleId, testTagId);

        // Fetch link
        List<Map<String, Object>> roleLinks = jdbcTemplate.queryForList(
                "SELECT * FROM role_schema_tags WHERE role_id = ? AND schema_tag_id = ?", testRoleId, testTagId);
        assertEquals(1, roleLinks.size());
    }

    @Test
    public void testDocumentMetadataSchemaAndConstraints() {
        UUID docId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)", catId, "Category " + catId);

        // Insert valid metadata
        jdbcTemplate.update("INSERT INTO documents (id, category_id, title, document_type, academic_year, status, program, process, approval_date, document_number, responsible_name, responsible_title, responsible_unit) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                docId, catId, "Valid Document", "Position", "2026–2027", "ACTIVE", "postgraduate", "stipends", java.sql.Date.valueOf("2026-09-01"), "NUM-456", "Иван Иванов", "Декан", "Аспирантура");

        List<Map<String, Object>> docs = jdbcTemplate.queryForList("SELECT * FROM documents WHERE id = ?", docId);
        assertEquals(1, docs.size());
        assertEquals("Position", docs.get(0).get("document_type"));
        assertEquals("2026–2027", docs.get(0).get("academic_year"));
        assertEquals("ACTIVE", docs.get(0).get("status"));
        assertEquals("postgraduate", docs.get(0).get("program"));
        assertEquals("stipends", docs.get(0).get("process"));
        assertEquals("NUM-456", docs.get(0).get("document_number"));
        assertEquals("Иван Иванов", docs.get(0).get("responsible_name"));

        // Verify invalid status constraint
        try {
            jdbcTemplate.update("INSERT INTO documents (id, category_id, title, status) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), catId, "Invalid Status Doc", "DRAFT");
            assertTrue(false, "Should have thrown exception due to status CHECK constraint");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("CHK_STATUS") || e.getMessage().contains("chk_status") || e.getMessage().contains("Constraint violation"));
        }

        // Verify invalid document type constraint
        try {
            jdbcTemplate.update("INSERT INTO documents (id, category_id, title, document_type) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), catId, "Invalid Type Doc", "Regulation");
            assertTrue(false, "Should have thrown exception due to document_type CHECK constraint");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("CHK_DOCUMENT_TYPE") || e.getMessage().contains("chk_document_type") || e.getMessage().contains("Constraint violation"));
        }

        // Verify invalid program constraint
        try {
            jdbcTemplate.update("INSERT INTO documents (id, category_id, title, program) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), catId, "Invalid Program Doc", "undergraduate");
            assertTrue(false, "Should have thrown exception due to program CHECK constraint");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("CHK_PROGRAM") || e.getMessage().contains("chk_program") || e.getMessage().contains("Constraint violation"));
        }

        // Verify invalid process constraint
        try {
            jdbcTemplate.update("INSERT INTO documents (id, category_id, title, process) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), catId, "Invalid Process Doc", "finance");
            assertTrue(false, "Should have thrown exception due to process CHECK constraint");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("CHK_PROCESS") || e.getMessage().contains("chk_process") || e.getMessage().contains("Constraint violation"));
        }
    }
}
