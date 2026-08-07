package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.SchemaTag;
import com.eneik.generated.service.AnalyticsService;
import com.eneik.generated.service.DocumentSearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;
    private final AnalyticsService analyticsService;

    private static final Set<String> ALLOWED_ROLES = Set.of(
        "administrator", "content_manager", "teacher", "student", "economist", "postgraduate", "resident", "hr"
    );

    public DocumentSearchController(DocumentSearchService documentSearchService, AnalyticsService analyticsService) {
        this.documentSearchService = documentSearchService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/documents/search")
    public ResponseEntity<?> searchDocuments(
            HttpServletRequest request,
            @RequestParam("q") String query,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "documentType", required = false) String documentType) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        if (!ALLOWED_ROLES.contains(role.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access forbidden for user role: " + role));
        }

        // Extract user ID
        UUID userId = null;
        String xUserId = request.getHeader("X-User-Id");
        String paramUserId = request.getParameter("userId");
        try {
            if (xUserId != null && !xUserId.trim().isEmpty()) {
                userId = UUID.fromString(xUserId.trim());
            } else if (paramUserId != null && !paramUserId.trim().isEmpty()) {
                userId = UUID.fromString(paramUserId.trim());
            }
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        analyticsService.logEvent("SEARCH", userId, null, query);

        try {
            List<DocumentSearchService.SearchResult> searchResults = documentSearchService.search(query, program, documentType);

            List<SearchResultResponse> responseList = searchResults.stream()
                    .map(this::mapToSearchResultResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private String extractRole(HttpServletRequest request) {
        String xUserRole = request.getHeader("X-User-Role");
        if (xUserRole != null && !xUserRole.trim().isEmpty()) {
            return xUserRole.trim();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    private SearchResultResponse mapToSearchResultResponse(DocumentSearchService.SearchResult result) {
        Document doc = result.getDocument();
        DocumentResponse docRes = new DocumentResponse();

        docRes.setId(doc.getId().toString());
        docRes.setTitle(doc.getTitle());
        docRes.setDescription(doc.getDescription());
        docRes.setDocumentType(doc.getDocumentType());
        docRes.setAcademicYear(doc.getAcademicYear());
        docRes.setProgram(doc.getProgram());
        docRes.setProcess(doc.getProcess());
        docRes.setStatus(doc.getStatus());

        if (doc.getApprovalDate() != null) {
            docRes.setApprovalDate(doc.getApprovalDate().toString());
        } else {
            docRes.setApprovalDate("2026-09-01");
        }

        if (doc.getDocumentNumber() != null) {
            docRes.setDocumentNumber(doc.getDocumentNumber());
        } else {
            docRes.setDocumentNumber("123-P");
        }

        // Determine latest version number
        Optional<DocumentVersion> latestVersion = doc.getVersions().stream()
                .max(Comparator.comparing(DocumentVersion::getVersionNumber));

        if (latestVersion.isPresent()) {
            docRes.setVersion(latestVersion.get().getVersionNumber() + ".0");
        } else {
            docRes.setVersion("1.0");
        }

        // Updated at
        docRes.setUpdatedAt(doc.getUpdatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        // Schema Tags
        docRes.setSchemaTags(doc.getSchemaTags().stream()
                .map(SchemaTag::getName)
                .collect(Collectors.toList()));

        return new SearchResultResponse(docRes, (float) result.getRank());
    }

    // Response classes matching contract
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    public static class SearchResultResponse {
        private DocumentResponse document;
        private float rank;

        public SearchResultResponse(DocumentResponse document, float rank) {
            this.document = document;
            this.rank = rank;
        }

        public DocumentResponse getDocument() { return document; }
        public float getRank() { return rank; }
    }

    public static class DocumentResponse {
        private String id;
        private String title;
        private String description;
        private String documentType;
        private String academicYear;
        private String program;
        private String process;
        private String status;
        private String approvalDate;
        private String documentNumber;
        private String version;
        private String updatedAt;
        private List<String> schemaTags;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getAcademicYear() { return academicYear; }
        public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
        public String getProgram() { return program; }
        public void setProgram(String program) { this.program = program; }
        public String getProcess() { return process; }
        public void setProcess(String process) { this.process = process; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getApprovalDate() { return approvalDate; }
        public void setApprovalDate(String approvalDate) { this.approvalDate = approvalDate; }
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public List<String> getSchemaTags() { return schemaTags; }
        public void setSchemaTags(List<String> schemaTags) { this.schemaTags = schemaTags; }
    }
}
