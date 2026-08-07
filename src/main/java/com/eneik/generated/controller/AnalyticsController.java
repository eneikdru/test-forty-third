package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final DocumentRepository documentRepository;

    public AnalyticsController(AnalyticsService analyticsService, DocumentRepository documentRepository) {
        this.analyticsService = analyticsService;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {

        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Document not found with ID: " + id));
        }

        Document doc = documentOpt.get();

        // Extract user ID
        UUID userId = null;
        try {
            if (xUserId != null && !xUserId.trim().isEmpty()) {
                userId = UUID.fromString(xUserId.trim());
            } else if (paramUserId != null && !paramUserId.trim().isEmpty()) {
                userId = UUID.fromString(paramUserId.trim());
            }
        } catch (IllegalArgumentException e) {
            // Log warning or proceed with null
        }

        // Log the download event
        analyticsService.logEvent("DOWNLOAD", userId, doc.getId(), null);

        // Return a mock byte stream representing the downloaded file
        String mockFileContent = "Document Content for title: " + doc.getTitle() + "\nDescription: " + doc.getDescription();
        byte[] fileBytes = mockFileContent.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "document-" + id + ".txt");

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/analytics/export")
    public ResponseEntity<byte[]> exportAnalytics(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "format", defaultValue = "CSV") String format) {

        byte[] reportBytes = analyticsService.generateExport(startDate, endDate, format);

        HttpHeaders headers = new HttpHeaders();
        String filename = "analytics-export." + format.toLowerCase();

        if ("PDF".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
        } else if ("DOCX".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDispositionFormData("attachment", filename);
        } else if ("JSON".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
        } else {
            headers.setContentType(MediaType.valueOf("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
        }

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/analytics/download-stats")
    public ResponseEntity<?> getDownloadStats() {
        return ResponseEntity.ok(java.util.Map.of(
            "daily", java.util.List.of(
                java.util.Map.of("day", "Пн", "downloads", 12),
                java.util.Map.of("day", "Вт", "downloads", 18),
                java.util.Map.of("day", "Ср", "downloads", 15),
                java.util.Map.of("day", "Чт", "downloads", 32),
                java.util.Map.of("day", "Пт", "downloads", 24),
                java.util.Map.of("day", "Сб", "downloads", 8),
                java.util.Map.of("day", "Вс", "downloads", 5)
            ),
            "popular", java.util.List.of(
                java.util.Map.of("title", "Положение о ГИА ординаторов", "downloads", 45),
                java.util.Map.of("title", "Порядок приёма на 2026-2027", "downloads", 38),
                java.util.Map.of("title", "Положение о стипендиальном обеспечении", "downloads", 27),
                java.util.Map.of("title", "Положение об индивидуальном учёте", "downloads", 19)
            )
        ));
    }

    // Helper error response class
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
}
