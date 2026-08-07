package com.eneik.generated.service;

import com.eneik.generated.model.AnalyticsEvent;
import com.eneik.generated.model.Document;
import com.eneik.generated.repository.AnalyticsEventRepository;
import com.eneik.generated.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final DocumentRepository documentRepository;

    // Injectable/seedable providers for reproducible testing
    private Supplier<UUID> uuidProvider = UUID::randomUUID;
    private Supplier<LocalDateTime> dateTimeProvider = LocalDateTime::now;

    public AnalyticsService(AnalyticsEventRepository analyticsEventRepository,
                            DocumentRepository documentRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.documentRepository = documentRepository;
    }

    public void setUuidProvider(Supplier<UUID> uuidProvider) {
        this.uuidProvider = uuidProvider;
    }

    public void setDateTimeProvider(Supplier<LocalDateTime> dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
    }

    @Transactional
    public AnalyticsEvent logEvent(String eventType, UUID userId, UUID documentId, String searchQuery) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setId(uuidProvider.get());
        event.setEventType(eventType.toUpperCase());
        event.setUserId(userId);
        event.setDocumentId(documentId);
        event.setSearchQuery(searchQuery);
        event.setCreatedAt(dateTimeProvider.get());
        return analyticsEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsEvent> getEvents(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return analyticsEventRepository.findEventsInDateRange(start, end);
    }

    @Transactional(readOnly = true)
    public byte[] generateExport(LocalDate startDate, LocalDate endDate, String format) {
        List<AnalyticsEvent> events = getEvents(startDate, endDate);

        // Map document titles for fast lookup
        Set<UUID> docIds = events.stream()
                .map(AnalyticsEvent::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> docTitleMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<Document> docs = documentRepository.findAllById(docIds);
            for (Document d : docs) {
                docTitleMap.put(d.getId(), d.getTitle());
            }
        }

        String reportType = format != null ? format.toUpperCase() : "CSV";

        if ("PDF".equals(reportType)) {
            return generatePdfExport(events, docTitleMap);
        } else if ("DOCX".equals(reportType)) {
            return generateDocxExport(events, docTitleMap);
        } else {
            return generateCsvExport(events, docTitleMap);
        }
    }

    private byte[] generateCsvExport(List<AnalyticsEvent> events, Map<UUID, String> docTitleMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event ID,Event Type,User ID,Document ID,Document Title,Search Query,Created At\n");

        for (AnalyticsEvent e : events) {
            String docTitle = e.getDocumentId() != null ? docTitleMap.getOrDefault(e.getDocumentId(), "Unknown") : "";
            sb.append(escapeCsv(e.getId().toString())).append(",")
              .append(escapeCsv(e.getEventType())).append(",")
              .append(escapeCsv(e.getUserId() != null ? e.getUserId().toString() : "")).append(",")
              .append(escapeCsv(e.getDocumentId() != null ? e.getDocumentId().toString() : "")).append(",")
              .append(escapeCsv(docTitle)).append(",")
              .append(escapeCsv(e.getSearchQuery() != null ? e.getSearchQuery() : "")).append(",")
              .append(escapeCsv(e.getCreatedAt().toString())).append("\n");
        }

        return sb.toString().getBytes();
    }

    private byte[] generatePdfExport(List<AnalyticsEvent> events, Map<UUID, String> docTitleMap) {
        // Minimal PDF format structure containing the analytics table
        StringBuilder content = new StringBuilder();
        content.append("EIOS Analytics Export Report\n");
        content.append("Generated At: ").append(dateTimeProvider.get()).append("\n\n");
        content.append(String.format("%-36s | %-10s | %-36s | %-30s\n", "Event ID", "Type", "Document ID", "Document Title"));
        content.append("-".repeat(120)).append("\n");

        for (AnalyticsEvent e : events) {
            String docTitle = e.getDocumentId() != null ? docTitleMap.getOrDefault(e.getDocumentId(), "Unknown") : "N/A";
            if (docTitle.length() > 30) {
                docTitle = docTitle.substring(0, 27) + "...";
            }
            content.append(String.format("%-36s | %-10s | %-36s | %-30s\n",
                    e.getId(),
                    e.getEventType(),
                    e.getDocumentId() != null ? e.getDocumentId().toString() : "N/A",
                    docTitle
            ));
        }

        // Wrap plain text report inside a basic valid PDF/binary structure
        String text = content.toString();
        String pdfString = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n" +
                "endobj\n" +
                "5 0 obj\n" +
                "<< /Length " + text.length() + " >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 10 Tf\n" +
                "50 750 Td\n" +
                "12 TL\n" +
                escapePdfText(text) +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000058 00000 n \n" +
                "0000000115 00000 n \n" +
                "0000000212 00000 n \n" +
                "0000000281 00000 n \n" +
                "trailer\n" +
                "<< /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "450\n" +
                "%%EOF";

        return pdfString.getBytes();
    }

    private byte[] generateDocxExport(List<AnalyticsEvent> events, Map<UUID, String> docTitleMap) {
        // Simple plain text representations for DOCX formatted stream
        StringBuilder content = new StringBuilder();
        content.append("EIOS Analytics Export Report (DOCX Format)\n");
        content.append("Generated At: ").append(dateTimeProvider.get()).append("\n\n");
        for (AnalyticsEvent e : events) {
            String docTitle = e.getDocumentId() != null ? docTitleMap.getOrDefault(e.getDocumentId(), "Unknown") : "N/A";
            content.append("ID: ").append(e.getId())
                   .append(" | Type: ").append(e.getEventType())
                   .append(" | Document: ").append(docTitle)
                   .append(" | Time: ").append(e.getCreatedAt()).append("\n");
        }
        return content.toString().getBytes();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String escapePdfText(String text) {
        // PDF stream text strings should escape parentheses and map newlines to Tj operations
        String[] lines = text.split("\n");
        return Arrays.stream(lines)
                .map(line -> "(" + line.replace("(", "\\(").replace(")", "\\)") + ") Tj T*")
                .collect(Collectors.joining("\n"));
    }
}
