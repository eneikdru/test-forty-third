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

        // Extended tables
        assertTrue(tablesStr.contains("external_lms_metadata"));
        assertTrue(tablesStr.contains("telegram_user_preferences"));
        assertTrue(tablesStr.contains("max_user_preferences"));
        assertTrue(tablesStr.contains("notification_subscriptions"));
        assertTrue(tablesStr.contains("analytics_events"));
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
    public void testExtendedSchemaFunctionality() {
        UUID catId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID lmsMetaId = UUID.randomUUID();
        UUID telegramUserId = UUID.randomUUID();
        UUID maxUserId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        // 1. Insert category & document
        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)", catId, "LMS Testing Category");
        jdbcTemplate.update("INSERT INTO documents (id, category_id, title) VALUES (?, ?, ?)", docId, catId, "LMS Sync Document");

        // 2. Insert external LMS metadata
        jdbcTemplate.update("INSERT INTO external_lms_metadata (id, document_id, lms_name, external_id, resource_url, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)", lmsMetaId, docId, "Moodle", "moodle-course-123", "https://sdo.crie.ru/course/view.php?id=123", "SYNCED");

        // 3. Insert Telegram & Max User Preferences
        jdbcTemplate.update("INSERT INTO telegram_user_preferences (user_id, telegram_chat_id, is_enabled) VALUES (?, ?, ?)",
                telegramUserId, "chat-8888", true);
        jdbcTemplate.update("INSERT INTO max_user_preferences (user_id, max_user_id, is_enabled) VALUES (?, ?, ?)",
                maxUserId, "max-9999", false);

        // 4. Insert notification subscription
        jdbcTemplate.update("INSERT INTO notification_subscriptions (id, user_id, channel, category_id) VALUES (?, ?, ?, ?)",
                subId, telegramUserId, "TELEGRAM", catId);

        // 5. Insert analytics events
        jdbcTemplate.update("INSERT INTO analytics_events (id, event_type, user_id, document_id, search_query) VALUES (?, ?, ?, ?, ?)",
                eventId, "DOWNLOAD", telegramUserId, docId, null);

        // Verify counts and data
        Map<String, Object> lmsMeta = jdbcTemplate.queryForMap("SELECT * FROM external_lms_metadata WHERE id = ?", lmsMetaId);
        assertEquals("Moodle", lmsMeta.get("lms_name"));
        assertEquals("moodle-course-123", lmsMeta.get("external_id"));

        Map<String, Object> tgPref = jdbcTemplate.queryForMap("SELECT * FROM telegram_user_preferences WHERE user_id = ?", telegramUserId);
        assertEquals("chat-8888", tgPref.get("telegram_chat_id"));
        assertEquals(true, tgPref.get("is_enabled"));

        Map<String, Object> maxPref = jdbcTemplate.queryForMap("SELECT * FROM max_user_preferences WHERE user_id = ?", maxUserId);
        assertEquals("max-9999", maxPref.get("max_user_id"));
        assertEquals(false, maxPref.get("is_enabled"));

        Map<String, Object> sub = jdbcTemplate.queryForMap("SELECT * FROM notification_subscriptions WHERE id = ?", subId);
        assertEquals("TELEGRAM", sub.get("channel"));

        Map<String, Object> event = jdbcTemplate.queryForMap("SELECT * FROM analytics_events WHERE id = ?", eventId);
        assertEquals("DOWNLOAD", event.get("event_type"));
    }
}
