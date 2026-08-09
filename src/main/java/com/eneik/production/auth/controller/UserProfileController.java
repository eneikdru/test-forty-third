package com.eneik.production.auth.controller;

import com.eneik.production.auth.model.User;
import com.eneik.production.auth.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping({"/api/v1/profile/preferences", "/api/v1/users/preferences", "/api/v1/users/profile/preferences"})
public class UserProfileController {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getPreferences(HttpServletRequest request) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Пользователь не авторизован"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Пользователь не найден"));
        }

        User user = userOpt.get();
        Map<String, Object> prefs = parsePreferences(user.getPreferences());
        return ResponseEntity.ok(prefs);
    }

    @PostMapping
    public ResponseEntity<?> updatePreferences(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Пользователь не авторизован"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Пользователь не найден"));
        }

        User user = userOpt.get();
        Map<String, Object> currentPrefs = parsePreferences(user.getPreferences());

        if (body.containsKey("savedSearches")) {
            currentPrefs.put("savedSearches", body.get("savedSearches"));
        }
        if (body.containsKey("saved_searches")) {
            currentPrefs.put("savedSearches", body.get("saved_searches"));
        }

        if (body.containsKey("favorites")) {
            currentPrefs.put("favorites", body.get("favorites"));
        }
        if (body.containsKey("favorite_documents")) {
            currentPrefs.put("favorites", body.get("favorite_documents"));
        }

        try {
            user.setPreferences(objectMapper.writeValueAsString(currentPrefs));
            userRepository.save(user);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Ошибка сохранения настроек"));
        }

        return ResponseEntity.ok(currentPrefs);
    }

    @PostMapping("/save-search")
    public ResponseEntity<?> saveSearch(HttpServletRequest request, @RequestBody Map<String, String> body) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Пользователь не авторизован"));
        }

        String query = null;
        if (body != null) {
            if (body.containsKey("query")) {
                query = body.get("query");
            } else if (body.containsKey("savedSearch")) {
                query = body.get("savedSearch");
            } else if (body.containsKey("search")) {
                query = body.get("search");
            }
        }

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "Параметр 'query' обязателен"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Пользователь не найден"));
        }

        User user = userOpt.get();
        Map<String, Object> currentPrefs = parsePreferences(user.getPreferences());
        List<String> savedSearches = getList(currentPrefs, "savedSearches");

        if (!savedSearches.contains(query.trim())) {
            savedSearches.add(query.trim());
        }
        currentPrefs.put("savedSearches", savedSearches);

        try {
            user.setPreferences(objectMapper.writeValueAsString(currentPrefs));
            userRepository.save(user);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Ошибка сохранения настроек"));
        }

        return ResponseEntity.ok(currentPrefs);
    }

    @PostMapping("/favorite")
    public ResponseEntity<?> saveFavorite(HttpServletRequest request, @RequestBody Map<String, String> body) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Пользователь не авторизован"));
        }

        String documentId = null;
        if (body != null) {
            if (body.containsKey("documentId")) {
                documentId = body.get("documentId");
            } else if (body.containsKey("favorite")) {
                documentId = body.get("favorite");
            } else if (body.containsKey("id")) {
                documentId = body.get("id");
            }
        }

        if (documentId == null || documentId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "Параметр 'documentId' обязателен"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", "Пользователь не найден"));
        }

        User user = userOpt.get();
        Map<String, Object> currentPrefs = parsePreferences(user.getPreferences());
        List<String> favorites = getList(currentPrefs, "favorites");

        if (!favorites.contains(documentId.trim())) {
            favorites.add(documentId.trim());
        }
        currentPrefs.put("favorites", favorites);

        try {
            user.setPreferences(objectMapper.writeValueAsString(currentPrefs));
            userRepository.save(user);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Ошибка сохранения настроек"));
        }

        return ResponseEntity.ok(currentPrefs);
    }

    private UUID extractUserId(HttpServletRequest request) {
        Object validatedUserId = request.getAttribute("X-User-Id");
        if (validatedUserId != null) {
            try {
                return UUID.fromString((String) validatedUserId);
            } catch (IllegalArgumentException e) {
                // Ignore and proceed
            }
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Session-Invalid"))) {
            return null;
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Allow-Fallback"))) {
            String xUserId = request.getHeader("X-User-Id");
            if (xUserId != null && !xUserId.trim().isEmpty()) {
                try {
                    return UUID.fromString(xUserId.trim());
                } catch (IllegalArgumentException e) {
                    // Ignore and proceed
                }
            }
        }
        return null;
    }

    private Map<String, Object> parsePreferences(String json) {
        if (json == null || json.trim().isEmpty()) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("savedSearches", new ArrayList<>());
            defaults.put("favorites", new ArrayList<>());
            return defaults;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
            if (!map.containsKey("savedSearches")) {
                map.put("savedSearches", new ArrayList<>());
            }
            if (!map.containsKey("favorites")) {
                map.put("favorites", new ArrayList<>());
            }
            return map;
        } catch (IOException e) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("savedSearches", new ArrayList<>());
            defaults.put("favorites", new ArrayList<>());
            return defaults;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        if (obj instanceof List) {
            return new ArrayList<>((List<String>) obj);
        }
        return new ArrayList<>();
    }
}
