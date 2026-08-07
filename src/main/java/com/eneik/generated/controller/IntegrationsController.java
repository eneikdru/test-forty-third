package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentLmsMetadata;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.DocumentLmsMetadataRepository;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationsController {

    private final UserRoleRepository userRoleRepository;
    private final DocumentRepository documentRepository;
    private final DocumentLmsMetadataRepository documentLmsMetadataRepository;
    private final ObjectMapper objectMapper;

    @Value("${notification.internal.service.key:INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    public IntegrationsController(UserRoleRepository userRoleRepository,
                                  DocumentRepository documentRepository,
                                  DocumentLmsMetadataRepository documentLmsMetadataRepository,
                                  ObjectMapper objectMapper) {
        this.userRoleRepository = userRoleRepository;
        this.documentRepository = documentRepository;
        this.documentLmsMetadataRepository = documentLmsMetadataRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/eios/auth/sync")
    @Transactional
    public ResponseEntity<?> syncEiosRoles(
            HttpServletRequest request,
            @RequestBody EiosRoleSyncRequest payload) {
        if (!isAuthorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Missing or invalid internal service bearer token"));
        }

        if (payload.getUserId() == null || payload.getRoles() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "userId and roles are required"));
        }

        UUID userId = payload.getUserId();

        // Clear old roles
        userRoleRepository.deleteByUserId(userId);

        // Add new roles
        int count = 0;
        for (String roleName : payload.getRoles()) {
            if (roleName != null && !roleName.trim().isEmpty()) {
                UserRole userRole = new UserRole(UUID.randomUUID(), userId, roleName.trim());
                userRoleRepository.save(userRole);
                count++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "syncedRolesCount", count
        ));
    }

    @PostMapping("/lms/webhooks")
    @Transactional
    public ResponseEntity<?> processLmsWebhook(
            HttpServletRequest request,
            @RequestBody LmsWebhookPayload webhookPayload) {
        if (!isAuthorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Missing or invalid internal service bearer token"));
        }

        if (webhookPayload.getProvider() == null || webhookPayload.getEventType() == null || webhookPayload.getDocumentId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "provider, eventType, and documentId are required"));
        }

        Optional<Document> docOpt = documentRepository.findById(webhookPayload.getDocumentId());
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "Document not found: " + webhookPayload.getDocumentId()));
        }

        Document doc = docOpt.get();
        String provider = webhookPayload.getProvider();
        UUID documentId = doc.getId();

        // Check if there is an existing DocumentLmsMetadata for this (documentId, provider)
        List<DocumentLmsMetadata> existingMetadataList = documentLmsMetadataRepository.findByDocumentId(documentId);
        DocumentLmsMetadata metadata = existingMetadataList.stream()
                .filter(m -> provider.equalsIgnoreCase(m.getLmsProvider()))
                .findFirst()
                .orElse(null);

        Map<String, Object> payloadMap = webhookPayload.getPayload();
        String externalId = null;
        String externalUrl = null;
        String metadataJson = null;

        if (payloadMap != null) {
            if (payloadMap.containsKey("externalId")) {
                externalId = String.valueOf(payloadMap.get("externalId"));
            } else if (payloadMap.containsKey("external_id")) {
                externalId = String.valueOf(payloadMap.get("external_id"));
            }

            if (payloadMap.containsKey("externalUrl")) {
                externalUrl = String.valueOf(payloadMap.get("externalUrl"));
            } else if (payloadMap.containsKey("external_url")) {
                externalUrl = String.valueOf(payloadMap.get("external_url"));
            }

            try {
                metadataJson = objectMapper.writeValueAsString(payloadMap);
            } catch (Exception e) {
                metadataJson = payloadMap.toString();
            }
        }

        if (externalId == null || externalId.trim().isEmpty()) {
            externalId = provider + "-ext-" + documentId.toString();
        }
        if (externalUrl == null || externalUrl.trim().isEmpty()) {
            externalUrl = "https://" + provider.toLowerCase() + ".crie.ru/resource/" + externalId;
        }

        if (metadata == null) {
            metadata = new DocumentLmsMetadata(
                    UUID.randomUUID(),
                    doc,
                    provider,
                    externalId,
                    externalUrl,
                    metadataJson
            );
        } else {
            metadata.setExternalId(externalId);
            metadata.setExternalUrl(externalUrl);
            metadata.setMetadataJson(metadataJson);
            metadata.setUpdatedAt(java.time.LocalDateTime.now());
        }

        documentLmsMetadataRepository.save(metadata);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Webhook processed successfully"
        ));
    }

    private boolean isAuthorized(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return internalServiceKey.equals(token);
        }
        return false;
    }

    // DTO classes matching contract
    public static class EiosRoleSyncRequest {
        private UUID userId;
        private List<String> roles;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class LmsWebhookPayload {
        private String provider;
        private String eventType;
        private UUID documentId;
        private Map<String, Object> payload;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public UUID getDocumentId() { return documentId; }
        public void setDocumentId(UUID documentId) { this.documentId = documentId; }

        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }
}
