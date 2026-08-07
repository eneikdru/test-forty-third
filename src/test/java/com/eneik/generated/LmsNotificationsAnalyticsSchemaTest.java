package com.eneik.generated;

import com.eneik.generated.model.AnalyticsEvent;
import com.eneik.generated.model.Category;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentLmsMetadata;
import com.eneik.generated.model.UserNotificationPreference;
import com.eneik.generated.repository.AnalyticsEventRepository;
import com.eneik.generated.repository.CategoryRepository;
import com.eneik.generated.repository.DocumentLmsMetadataRepository;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class LmsNotificationsAnalyticsSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentLmsMetadataRepository documentLmsMetadataRepository;

    @Autowired
    private UserNotificationPreferenceRepository userNotificationPreferenceRepository;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

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
    @Transactional
    public void testJPAEntitiesAndRepositories() {
        // Create parent Category & Document
        UUID catId = UUID.nameUUIDFromBytes("test-cat-jpa".getBytes());
        Category category = new Category(catId, "JPA Category");
        categoryRepository.save(category);

        UUID docId = UUID.nameUUIDFromBytes("test-doc-jpa".getBytes());
        Document document = new Document(docId, category, "JPA Document Title", "JPA Document Description");
        documentRepository.save(document);

        // 1. Test DocumentLmsMetadataRepository
        UUID lmsMetaId = UUID.nameUUIDFromBytes("test-lms-jpa".getBytes());
        DocumentLmsMetadata lmsMetadata = new DocumentLmsMetadata(
                lmsMetaId,
                document,
                "Teachbase",
                "tb_course_999",
                "https://teachbase.ru/course/999",
                "{\"tags\": [\"finance\", \"budget\"]}"
        );
        documentLmsMetadataRepository.save(lmsMetadata);

        Optional<DocumentLmsMetadata> retrievedLms = documentLmsMetadataRepository.findById(lmsMetaId);
        assertTrue(retrievedLms.isPresent());
        assertEquals("Teachbase", retrievedLms.get().getLmsProvider());
        assertEquals("tb_course_999", retrievedLms.get().getExternalId());
        assertEquals("https://teachbase.ru/course/999", retrievedLms.get().getExternalUrl());
        assertEquals("{\"tags\": [\"finance\", \"budget\"]}", retrievedLms.get().getMetadataJson());

        List<DocumentLmsMetadata> listByDoc = documentLmsMetadataRepository.findByDocumentId(docId);
        assertEquals(1, listByDoc.size());
        assertEquals(lmsMetaId, listByDoc.get(0).getId());

        // 2. Test UserNotificationPreferenceRepository
        UUID prefId = UUID.nameUUIDFromBytes("test-pref-jpa".getBytes());
        UUID userId = UUID.nameUUIDFromBytes("test-user-jpa".getBytes());
        UserNotificationPreference pref = new UserNotificationPreference(
                prefId,
                userId,
                "987654321",
                "max_channel_xyz",
                true
        );
        userNotificationPreferenceRepository.save(pref);

        Optional<UserNotificationPreference> retrievedPref = userNotificationPreferenceRepository.findById(prefId);
        assertTrue(retrievedPref.isPresent());
        assertEquals("987654321", retrievedPref.get().getTelegramChatId());
        assertEquals("max_channel_xyz", retrievedPref.get().getMaxChatId());
        assertTrue(retrievedPref.get().getNotifyOnDocumentUpdate());

        Optional<UserNotificationPreference> retrievedByUserId = userNotificationPreferenceRepository.findByUserId(userId);
        assertTrue(retrievedByUserId.isPresent());
        assertEquals(prefId, retrievedByUserId.get().getId());

        // 3. Test AnalyticsEventRepository
        UUID eventId = UUID.nameUUIDFromBytes("test-event-jpa".getBytes());
        AnalyticsEvent event = new AnalyticsEvent(
                eventId,
                "DOWNLOAD",
                userId,
                document,
                null
        );
        analyticsEventRepository.save(event);

        Optional<AnalyticsEvent> retrievedEvent = analyticsEventRepository.findById(eventId);
        assertTrue(retrievedEvent.isPresent());
        assertEquals("DOWNLOAD", retrievedEvent.get().getEventType());
        assertEquals(userId, retrievedEvent.get().getUserId());
        assertNotNull(retrievedEvent.get().getDocument());
        assertEquals(docId, retrievedEvent.get().getDocument().getId());

        List<AnalyticsEvent> eventsByType = analyticsEventRepository.findByEventType("DOWNLOAD");
        assertTrue(eventsByType.size() >= 1);

        List<AnalyticsEvent> eventsByDoc = analyticsEventRepository.findByDocumentId(docId);
        assertEquals(1, eventsByDoc.size());
        assertEquals(eventId, eventsByDoc.get(0).getId());
    }
}
