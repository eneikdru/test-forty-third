package com.eneik.production.auth;

import com.eneik.generated.Application;
import com.eneik.production.auth.model.User;
import com.eneik.production.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Application.class, properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserProfilePreferencesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private User testUser;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        userId = UUID.randomUUID();
        testUser = new User(
                userId,
                "preferences_test_user",
                "password_hash_dummy",
                LocalDateTime.now()
        );
        userRepository.save(testUser);
    }

    @Test
    public void testGetPreferencesWithNoInitialDataReturnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/profile/preferences")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasSize(0)))
                .andExpect(jsonPath("$.favorites", hasSize(0)));
    }

    @Test
    public void testUpdateAndGetPreferences() throws Exception {
        Map<String, Object> newPrefs = Map.of(
                "savedSearches", List.of("Кадровые регламенты", "Финансовые документы"),
                "favorites", List.of(UUID.randomUUID().toString())
        );

        mockMvc.perform(post("/api/v1/profile/preferences")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPrefs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasSize(2)))
                .andExpect(jsonPath("$.savedSearches", containsInAnyOrder("Кадровые регламенты", "Финансовые документы")))
                .andExpect(jsonPath("$.favorites", hasSize(1)));

        // Verify it persists on retrieval
        mockMvc.perform(get("/api/v1/profile/preferences")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasSize(2)))
                .andExpect(jsonPath("$.favorites", hasSize(1)));
    }

    @Test
    public void testSaveSearchAppendsAndPreventsDuplicates() throws Exception {
        // Save first search query
        mockMvc.perform(post("/api/v1/profile/preferences/save-search")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Поиск регламентов\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasItem("Поиск регламентов")));

        // Save duplicate search query
        mockMvc.perform(post("/api/v1/profile/preferences/save-search")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Поиск регламентов\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasSize(1)));

        // Save another search query
        mockMvc.perform(post("/api/v1/profile/preferences/save-search")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Положение об аттестации\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedSearches", hasSize(2)))
                .andExpect(jsonPath("$.savedSearches", containsInAnyOrder("Поиск регламентов", "Положение об аттестации")));
    }

    @Test
    public void testFavoriteAppendsAndPreventsDuplicates() throws Exception {
        String docId1 = UUID.randomUUID().toString();
        String docId2 = UUID.randomUUID().toString();

        // Favorite first document
        mockMvc.perform(post("/api/v1/profile/preferences/favorite")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + docId1 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorites", hasItem(docId1)));

        // Favorite duplicate document
        mockMvc.perform(post("/api/v1/profile/preferences/favorite")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + docId1 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorites", hasSize(1)));

        // Favorite another document
        mockMvc.perform(post("/api/v1/profile/preferences/favorite")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + docId2 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorites", hasSize(2)))
                .andExpect(jsonPath("$.favorites", containsInAnyOrder(docId1, docId2)));
    }

    @Test
    public void testAliasEndpointsWorkCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/users/preferences")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/profile/preferences")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "Student"))
                .andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedAccessReturnsLocalized401() throws Exception {
        mockMvc.perform(get("/api/v1/profile/preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Пользователь не авторизован")));
    }
}
