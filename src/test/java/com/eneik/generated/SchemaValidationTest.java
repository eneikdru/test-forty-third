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
}
