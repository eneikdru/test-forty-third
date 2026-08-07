package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SettingsAndAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetAndSetNotificationPreferences() throws Exception {
        // Fetch default preferences
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramChatId", is("123456789")))
                .andExpect(jsonPath("$.maxChatId", is("user_max_abc")))
                .andExpect(jsonPath("$.notifyOnDocumentUpdate", is(true)));

        // Update preferences
        String payload = "{\"telegramChatId\":\"my_new_channel\",\"maxChatId\":\"my_new_max\",\"notifyOnDocumentUpdate\":false}";
        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramChatId", is("my_new_channel")))
                .andExpect(jsonPath("$.maxChatId", is("my_new_max")))
                .andExpect(jsonPath("$.notifyOnDocumentUpdate", is(false)));

        // Verify updated preferences persist
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramChatId", is("my_new_channel")))
                .andExpect(jsonPath("$.maxChatId", is("my_new_max")))
                .andExpect(jsonPath("$.notifyOnDocumentUpdate", is(false)));
    }

    @Test
    public void testGetDownloadStats() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/download-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(7)))
                .andExpect(jsonPath("$.daily[0].day", is("Пн")))
                .andExpect(jsonPath("$.popular", hasSize(4)))
                .andExpect(jsonPath("$.popular[0].title", containsString("Положение")));
    }
}
