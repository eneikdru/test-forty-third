package com.eneik.generated;

import com.eneik.generated.model.*;
import com.eneik.generated.repository.*;
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
public class LmsNotificationsAnalyticsSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentLmsMetadataRepository lmsMetadataRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void testNewSchemaExists() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList("SHOW TABLES");
        String tablesStr = tables.toString().toLowerCase();

        assertTrue(tablesStr.contains("document_lms_metadata"));
        assertTrue(tablesStr.contains("user_notification_preferences"));
        assertTrue(tablesStr.contains("analytics_events"));
    }

    @Test
    public void testLmsMetadataInsertion() {
        UUID docId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO categories (id, name) VALUES (?, ?)", catId, "Category " + catId);
        jdbcTemplate.update("INSERT INTO documents (id, category_id, title) VALUES (?, ?, ?)", docId, catId, "Test Doc LMS");

        UUID metaId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO document_lms_metadata (id, document_id, lms_provider, external_id, external_url) VALUES (?, ?, ?, ?, ?)",
                metaId, docId, "Moodle", "course_123", "https://sdo.crie.ru/course/123");

        List<Map<String, Object>> metadata = jdbcTemplate.queryForList("SELECT * FROM document_lms_metadata WHERE id = ?", metaId);
        assertEquals(1, metadata.size());
        assertEquals("Moodle", metadata.get(0).get("lms_provider"));
    }

    @Test
    public void testNotificationPreferencesInsertion() {
        UUID prefId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO user_notification_preferences (id, user_id, telegram_chat_id, max_chat_id) VALUES (?, ?, ?, ?)",
                prefId, userId, "123456789", "user_max_abc");

        List<Map<String, Object>> prefs = jdbcTemplate.queryForList("SELECT * FROM user_notification_preferences WHERE id = ?", prefId);
        assertEquals(1, prefs.size());
        assertEquals("123456789", prefs.get(0).get("telegram_chat_id"));
    }

    @Test
    public void testLmsMetadataJpaPersistence() {
        UUID catId = UUID.randomUUID();
        Category category = new Category(catId, "Integration Category " + catId);
        categoryRepository.save(category);

        UUID docId = UUID.randomUUID();
        Document document = new Document();
        document.setId(docId);
        document.setCategory(category);
        document.setTitle("Test JPA Document LMS");
        documentRepository.save(document);

        UUID metaId = UUID.randomUUID();
        DocumentLmsMetadata metadata = new DocumentLmsMetadata(
            metaId,
            document,
            "Teachbase",
            "tb_course_999",
            "https://teachbase.ru/course/999",
            "{\"duration\": \"2 weeks\"}"
        );
        lmsMetadataRepository.save(metadata);

        List<DocumentLmsMetadata> fetchedList = lmsMetadataRepository.findByDocumentId(docId);
        assertEquals(1, fetchedList.size());
        DocumentLmsMetadata fetched = fetchedList.get(0);
        assertEquals(metaId, fetched.getId());
        assertEquals("Teachbase", fetched.getLmsProvider());
        assertEquals("tb_course_999", fetched.getExternalId());
        assertEquals("https://teachbase.ru/course/999", fetched.getExternalUrl());
        assertEquals("{\"duration\": \"2 weeks\"}", fetched.getMetadataJson());
    }
}
