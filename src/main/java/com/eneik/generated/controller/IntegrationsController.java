package com.eneik.generated.controller;

import com.eneik.generated.dto.EiosRoleSyncRequest;
import com.eneik.generated.dto.LmsWebhookPayload;
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

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationsController {

    private final DocumentRepository documentRepository;
    private final DocumentLmsMetadataRepository lmsMetadataRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;

    @Value("${notification.internal.service.key:INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    public IntegrationsController(DocumentRepository documentRepository,
                                  DocumentLmsMetadataRepository lmsMetadataRepository,
                                  UserRoleRepository userRoleRepository,
                                  ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.lmsMetadataRepository = lmsMetadataRepository;
        this.userRoleRepository = userRoleRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/lms/webhooks")
    @Transactional
    public ResponseEntity<?> processLmsWebhook(
            HttpServletRequest request,
            @RequestBody LmsWebhookPayload payload) {
        if (!isAuthorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Missing or invalid internal service bearer token"));
        }

        if (payload.getProvider() == null || payload.getEventType() == null || payload.getDocumentId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "provider, eventType, and documentId are required"));
        }

        Optional<Document> docOpt = documentRepository.findById(payload.getDocumentId());
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "Document not found with ID: " + payload.getDocumentId()));
        }

        Document document = docOpt.get();

        List<DocumentLmsMetadata> existingList = lmsMetadataRepository.findByDocumentId(document.getId());
        DocumentLmsMetadata metadata = existingList.stream()
                .filter(m -> m.getLmsProvider().equalsIgnoreCase(payload.getProvider()))
                .findFirst()
                .orElse(null);

        if (metadata == null) {
            metadata = new DocumentLmsMetadata();
            metadata.setId(UUID.randomUUID());
            metadata.setDocument(document);
            metadata.setLmsProvider(payload.getProvider());
            metadata.setCreatedAt(LocalDateTime.now());
        }

        metadata.setExternalId("lms_ext_" + UUID.randomUUID());
        metadata.setExternalUrl("https://sdo.crie.ru/course/" + payload.getDocumentId());

        String jsonString = "{}";
        if (payload.getPayload() != null) {
            try {
                jsonString = objectMapper.writeValueAsString(payload.getPayload());
            } catch (Exception e) {
                // fallback to toString representation if serialization fails
                jsonString = payload.getPayload().toString();
            }
        }
        metadata.setMetadataJson(jsonString);
        metadata.setUpdatedAt(LocalDateTime.now());

        lmsMetadataRepository.save(metadata);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed successfully"));
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

        // Delete old roles for this user
        userRoleRepository.deleteByUserId(payload.getUserId());

        // Save new roles
        for (String roleName : payload.getRoles()) {
            UserRole userRole = new UserRole(UUID.randomUUID(), payload.getUserId(), roleName);
            userRoleRepository.save(userRole);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "syncedRolesCount", payload.getRoles().size()
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
}
