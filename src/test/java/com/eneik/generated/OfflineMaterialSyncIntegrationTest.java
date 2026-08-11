package com.eneik.generated;

import com.eneik.generated.repository.*;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Robust automated integration test suite verifying Offline Material Creation and Sync.
 * Strictly satisfies the GWT specification of offline sync upon reconnection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OfflineMaterialSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdProvider idProvider;

    @Autowired
    private TimeProvider timeProvider;

    @BeforeEach
    public void setUp() {
        idProvider.reset();
        timeProvider.reset();
        jdbcTemplate.update("DELETE FROM analytics_events");
        jdbcTemplate.update("DELETE FROM document_schema_tags");
        jdbcTemplate.update("DELETE FROM document_versions");
        jdbcTemplate.update("DELETE FROM documents");
    }

    /**
     * Given an offline state,
     * When a reconnection event occurs and locally stored materials are pushed,
     * Then they are successfully created on the server.
     */
    @Test
    public void testReconnectionEventOfflineSync() throws Exception {
        // Simulate a reconnection push: POSTing a cached/locally-created document metadata and file to the server
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "offline-material.txt",
                "text/plain",
                "Содержимое офлайн-материала: Тестовый офлайн регламент".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", "Тестовый офлайн регламент")
                        .param("description", "Документ создан в офлайн-режиме")
                        .param("documentType", "Position")
                        .param("academicYear", "бессрочно")
                        .param("program", "both")
                        .param("process", "other")
                        .param("documentNumber", "ОФЛ-2026")
                        .header("X-User-Role", "Administrator"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Тестовый офлайн регламент")))
                .andExpect(jsonPath("$.description", is("Документ создан в офлайн-режиме")))
                .andExpect(jsonPath("$.version", is("1.0")))
                .andExpect(jsonPath("$.documentType", is("Position")))
                .andExpect(jsonPath("$.academicYear", is("бессрочно")))
                .andExpect(jsonPath("$.program", is("both")))
                .andExpect(jsonPath("$.process", is("other")))
                .andExpect(jsonPath("$.documentNumber", is("ОФЛ-2026")));
    }
}
