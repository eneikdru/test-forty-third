package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.SchemaTag;
import com.eneik.generated.model.DocumentComment;
import com.eneik.generated.model.DocumentActualizationRequest;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.DocumentVersionRepository;
import com.eneik.generated.repository.SchemaTagRepository;
import com.eneik.generated.repository.DocumentCommentRepository;
import com.eneik.generated.repository.DocumentActualizationRequestRepository;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.service.AnalyticsService;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.service.NotificationService;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.util.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentController.class);

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final SchemaTagRepository schemaTagRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final DocumentActualizationRequestRepository documentActualizationRequestRepository;
    private final IdProvider idProvider;
    private final TimeProvider timeProvider;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final NotificationDispatcher notificationDispatcher;

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of("Position", "Procedure", "Project", "Other");
    private static final Set<String> ALLOWED_PROGRAMS = Set.of("postgraduate", "residency", "both");
    private static final Set<String> ALLOWED_PROCESSES = Set.of("admission", "certification", "stipends", "practice", "result_tracking", "other");
    private static final Pattern ACADEMIC_YEAR_PATTERN = Pattern.compile("^(infinite|project|бессрочно|проект|\\d{4}-\\d{4}|\\d{4}–\\d{4})$");

    public DocumentController(DocumentRepository documentRepository,
                              DocumentVersionRepository documentVersionRepository,
                              SchemaTagRepository schemaTagRepository,
                              DocumentCommentRepository documentCommentRepository,
                              DocumentActualizationRequestRepository documentActualizationRequestRepository,
                              IdProvider idProvider,
                              TimeProvider timeProvider,
                              NotificationService notificationService,
                              AnalyticsService analyticsService,
                              NotificationDispatcher notificationDispatcher) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.schemaTagRepository = schemaTagRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.documentActualizationRequestRepository = documentActualizationRequestRepository;
        this.idProvider = idProvider;
        this.timeProvider = timeProvider;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.notificationDispatcher = notificationDispatcher;
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "academicYear", defaultValue = "infinite") String academicYear,
            @RequestParam("program") String program,
            @RequestParam("process") String process,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "schemaTags", required = false) List<String> schemaTags) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        // Access Control (RBAC): Content Managers and Administrators only
        boolean isAuthorized = role.equalsIgnoreCase("Administrator") ||
                role.equalsIgnoreCase("ADMINISTRATOR") ||
                role.equalsIgnoreCase("Content Manager") ||
                role.equalsIgnoreCase("CONTENT_MANAGER") ||
                role.equalsIgnoreCase("ContentManager");

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access forbidden for user role: " + role));
        }

        // Input validation
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Title is required"));
        }

        if (!ALLOWED_DOCUMENT_TYPES.contains(documentType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid documentType. Must be one of: " + ALLOWED_DOCUMENT_TYPES));
        }

        if (academicYear == null || !ACADEMIC_YEAR_PATTERN.matcher(academicYear).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid academicYear. Must match standard format or patterns"));
        }

        if (!ALLOWED_PROGRAMS.contains(program)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid program. Must be one of: " + ALLOWED_PROGRAMS));
        }

        if (!ALLOWED_PROCESSES.contains(process)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid process. Must be one of: " + ALLOWED_PROCESSES));
        }

        // Duplication control: check if a document with same documentNumber exists, or same title exists
        Document existingDoc = null;
        if (documentNumber != null && !documentNumber.trim().isEmpty()) {
            existingDoc = documentRepository.findAll().stream()
                    .filter(d -> documentNumber.trim().equalsIgnoreCase(d.getDocumentNumber()))
                    .findFirst()
                    .orElse(null);
        }
        if (existingDoc == null) {
            existingDoc = documentRepository.findAll().stream()
                    .filter(d -> title.trim().equalsIgnoreCase(d.getTitle()))
                    .findFirst()
                    .orElse(null);
        }

        Document finalDoc;
        int nextVersionNumber;

        if (existingDoc != null) {
            // Merge duplicate into existing document card with a new version
            finalDoc = existingDoc;
            nextVersionNumber = existingDoc.getVersions().stream()
                    .mapToInt(DocumentVersion::getVersionNumber)
                    .max()
                    .orElse(0) + 1;

            // Update metadata to new values if requested
            finalDoc.setUpdatedAt(timeProvider.now());
            if (description != null) {
                finalDoc.setDescription(description);
            }
            finalDoc.setDocumentType(documentType);
            finalDoc.setAcademicYear(academicYear);
            finalDoc.setProgram(program);
            finalDoc.setProcess(process);
        } else {
            // Create a new document
            finalDoc = new Document();
            finalDoc.setId(idProvider.generateUuid());
            finalDoc.setTitle(title.trim());
            finalDoc.setDescription(description);
            finalDoc.setDocumentType(documentType);
            finalDoc.setAcademicYear(academicYear);
            finalDoc.setProgram(program);
            finalDoc.setProcess(process);
            finalDoc.setDocumentNumber(documentNumber);
            finalDoc.setStatus("ACTIVE");
            finalDoc.setCreatedAt(timeProvider.now());
            finalDoc.setUpdatedAt(timeProvider.now());
            nextVersionNumber = 1;
        }

        // Populate and persist schema tags if present
        if (schemaTags != null && !schemaTags.isEmpty()) {
            Set<SchemaTag> tags = new HashSet<>();
            for (String tagName : schemaTags) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    SchemaTag tag = schemaTagRepository.findByName(tagName.trim())
                            .orElseGet(() -> {
                                SchemaTag newTag = new SchemaTag();
                                newTag.setId(idProvider.generateUuid());
                                newTag.setName(tagName.trim());
                                newTag.setDescription("Dynamically created schema tag: " + tagName);
                                return schemaTagRepository.save(newTag);
                            });
                    tags.add(tag);
                }
            }
            finalDoc.setSchemaTags(tags);
        }

        // Create new DocumentVersion
        DocumentVersion version = new DocumentVersion();
        version.setId(idProvider.generateUuid());
        version.setDocument(finalDoc);
        version.setVersionNumber(nextVersionNumber);

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            filename = "document.pdf";
        }
        version.setFileUrl("/api/files/" + finalDoc.getId() + "/v" + nextVersionNumber + "/" + filename);

        String fileType = "PDF";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot != -1) {
            fileType = filename.substring(lastDot + 1).toUpperCase();
        }
        version.setFileType(fileType);
        version.setStatus("ACTIVE");
        version.setAuthorName(role);
        version.setChangesSummary(nextVersionNumber == 1 ? "Initial upload" : "Uploaded version " + nextVersionNumber);
        version.setCreatedAt(timeProvider.now());

        // Bidirectional relationship management
        if (finalDoc.getVersions() == null) {
            finalDoc.setVersions(new HashSet<>());
        }
        finalDoc.getVersions().add(version);

        documentRepository.save(finalDoc);
        documentVersionRepository.save(version);

        // Fetch refreshed entity to ensure loaded versions are correct
        Document savedDoc = documentRepository.findById(finalDoc.getId()).orElse(finalDoc);

        try {
            notificationService.triggerNewVersionPublished(savedDoc.getId(), nextVersionNumber);
        } catch (Exception e) {
            log.error("Failed to trigger new version notification for document: " + savedDoc.getId(), e);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(savedDoc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        // Access Control: Only Administrators can delete
        boolean isAdmin = role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("ADMINISTRATOR");
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Only Administrators can delete or decommission documents"));
        }

        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Document not found"));
        }

        documentRepository.delete(docOpt.get());
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("")
    public ResponseEntity<?> getDocuments(
            HttpServletRequest request,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "process", required = false) String process,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        List<Document> documentsList = documentRepository.findAll();
        List<DocumentResponseDTO> responses = documentsList.stream()
                .filter(doc -> categoryId == null || (doc.getCategory() != null && doc.getCategory().getId().equals(categoryId)))
                .filter(doc -> program == null || program.equalsIgnoreCase(doc.getProgram()))
                .filter(doc -> process == null || process.equalsIgnoreCase(doc.getProcess()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        boolean hasPaginationParams = (page != null || size != null);
        int defaultPageSize = 10;
        int pageSize = (size != null) ? Math.max(1, size) : defaultPageSize;

        int pageIndex = 0;
        if (page != null) {
            pageIndex = (page > 1) ? (page - 1) : 0;
        }

        int offset = pageIndex * pageSize;
        int totalResults = responses.size();

        if (page != null && offset >= totalResults) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<DocumentResponseDTO> pageData;
        if (offset >= totalResults) {
            pageData = Collections.emptyList();
        } else {
            int toIndex = Math.min(offset + pageSize, totalResults);
            pageData = responses.subList(offset, toIndex);
        }

        if (hasPaginationParams || totalResults > pageSize) {
            return ResponseEntity.ok(new PaginatedResponse<>(totalResults, pageData));
        }

        return ResponseEntity.ok(responses);
    }

    public static class PaginatedResponse<T> {
        private long totalCount;
        private long total;
        private List<T> data;
        private List<T> results;
        private List<T> documents;

        public PaginatedResponse(long totalCount, List<T> data) {
            this.totalCount = totalCount;
            this.total = totalCount;
            this.data = data;
            this.results = data;
            this.documents = data;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public List<T> getData() { return data; }
        public void setData(List<T> data) { this.data = data; }
        public List<T> getResults() { return results; }
        public void setResults(List<T> results) { this.results = results; }
        public List<T> getDocuments() { return documents; }
        public void setDocuments(List<T> documents) { this.documents = documents; }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentDetails(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Document not found"));
        }

        Document doc = docOpt.get();

        // Extract user ID
        UUID userId = null;
        String xUserId = request.getHeader("X-User-Id");
        try {
            if (xUserId != null && !xUserId.trim().isEmpty()) {
                userId = UUID.fromString(xUserId.trim());
            }
        } catch (IllegalArgumentException e) {
            // Proceed with null
        }

        // Log VIEW event
        analyticsService.logEvent("VIEW", userId, doc.getId(), null);

        DocumentDetailsResponseDTO details = new DocumentDetailsResponseDTO();
        details.setDocument(mapToResponse(doc));

        List<DocumentVersionResponseDTO> versions = doc.getVersions().stream()
                .sorted(Comparator.comparing(DocumentVersion::getVersionNumber))
                .map(v -> {
                    DocumentVersionResponseDTO vDto = new DocumentVersionResponseDTO();
                    vDto.setId(v.getId().toString());
                    vDto.setVersionNumber(v.getVersionNumber());
                    vDto.setStatus(v.getStatus());
                    vDto.setChangesSummary(v.getChangesSummary());
                    vDto.setFileUrl(v.getFileUrl());
                    vDto.setCreatedAt(v.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                    return vDto;
                })
                .collect(Collectors.toList());

        details.setVersions(versions);
        return ResponseEntity.ok(details);
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

    private DocumentResponseDTO mapToResponse(Document doc) {
        DocumentResponseDTO res = new DocumentResponseDTO();
        res.setId(doc.getId().toString());
        res.setTitle(doc.getTitle());
        res.setDescription(doc.getDescription());
        res.setDocumentType(doc.getDocumentType());
        res.setAcademicYear(doc.getAcademicYear());
        res.setProgram(doc.getProgram());
        res.setProcess(doc.getProcess());
        res.setStatus(doc.getStatus());

        if (doc.getApprovalDate() != null) {
            res.setApprovalDate(doc.getApprovalDate().toString());
        }

        res.setDocumentNumber(doc.getDocumentNumber());

        Optional<DocumentVersion> latestVersion = doc.getVersions().stream()
                .max(Comparator.comparing(DocumentVersion::getVersionNumber));

        if (latestVersion.isPresent()) {
            res.setVersion(latestVersion.get().getVersionNumber() + ".0");
        } else {
            res.setVersion("1.0");
        }

        res.setUpdatedAt(doc.getUpdatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        res.setSchemaTags(doc.getSchemaTags().stream()
                .map(SchemaTag::getName)
                .collect(Collectors.toList()));

        return res;
    }

    private String escapeMarkdownV2(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '_' || c == '*' || c == '[' || c == ']' || c == '(' || c == ')' || c == '~' || c == '`' ||
                c == '>' || c == '#' || c == '+' || c == '=' || c == '|' || c == '{' || c == '}' || c == '.' ||
                c == '!' || c == '-' || c == '\\' || c == '\'') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        List<DocumentComment> list = documentCommentRepository.findByDocumentIdOrderByCreatedAtAsc(id);
        List<Map<String, Object>> comments = new ArrayList<>();
        for (DocumentComment dc : list) {
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", dc.getId().toString());
            comment.put("userId", dc.getUserId().toString());
            comment.put("userName", dc.getUserName());
            comment.put("text", dc.getText());
            comment.put("createdAt", dc.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            comments.add(comment);
        }
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> postComment(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Comment text is required"));
        }

        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Document not found"));
        }

        UUID commentUuid = idProvider.generateUuid();
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        DocumentComment dc = new DocumentComment();
        dc.setId(commentUuid);
        dc.setDocument(docOpt.get());
        dc.setUserId(userId);
        dc.setUserName(role);
        dc.setText(text);
        dc.setCreatedAt(timeProvider.now());
        documentCommentRepository.save(dc);

        Map<String, Object> comment = new HashMap<>();
        comment.put("id", commentUuid.toString());
        comment.put("userId", userId.toString());
        comment.put("userName", role);
        comment.put("text", text);
        comment.put("createdAt", dc.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        // Notify content managers via Telegram/Max Dispatcher
        TelegramNotificationRequest notifyRequest = new TelegramNotificationRequest();
        notifyRequest.setNotificationId(idProvider.generateNotificationId());
        notifyRequest.setEventType("comment.added");
        notifyRequest.setRecipientType("channel_or_chat");
        notifyRequest.setTargetId("@cniiep_edu_updates");
        notifyRequest.setTemplateLanguage("ru");
        notifyRequest.setMessageFormat("markdown_v2");

        TelegramNotificationRequest.PayloadDetails payload = new TelegramNotificationRequest.PayloadDetails();
        payload.setDocumentId(id.toString());
        payload.setTitle(docOpt.get().getTitle());
        payload.setActionType("комментарий");
        payload.setAuthorName(role);
        payload.setUpdateSummary("Добавлен комментарий: " + text);
        payload.setDirectLink("https://kb.crie.ru/documents/" + id);
        notifyRequest.setPayload(payload);

        String renderedMessage = "💬 *Новый комментарий к документу в Базе Знаний*\n\n" +
                "📝 *Документ:* " + escapeMarkdownV2(docOpt.get().getTitle()) + "\n" +
                "👤 *Автор:* " + escapeMarkdownV2(role) + "\n" +
                "💬 *Текст:* " + escapeMarkdownV2(text);
        notifyRequest.setRenderedMessage(renderedMessage);

        notificationDispatcher.dispatchTelegram(notifyRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PostMapping("/{id}/actualization-requests")
    public ResponseEntity<?> postActualizationRequest(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Actualization reason is required"));
        }

        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Document not found"));
        }

        UUID reqUuid = idProvider.generateUuid();
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DocumentActualizationRequest dar = new DocumentActualizationRequest();
        dar.setId(reqUuid);
        dar.setDocument(docOpt.get());
        dar.setRequesterId(requesterId);
        dar.setReason(reason);
        dar.setStatus("PENDING");
        dar.setCreatedAt(timeProvider.now());
        documentActualizationRequestRepository.save(dar);

        Map<String, Object> req = new HashMap<>();
        req.put("id", reqUuid.toString());
        req.put("documentId", id.toString());
        req.put("requesterId", requesterId.toString());
        req.put("reason", reason);
        req.put("status", "PENDING");
        req.put("createdAt", dar.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        // Notify content managers via Telegram/Max Dispatcher
        TelegramNotificationRequest notifyRequest = new TelegramNotificationRequest();
        notifyRequest.setNotificationId(idProvider.generateNotificationId());
        notifyRequest.setEventType("actualization.requested");
        notifyRequest.setRecipientType("channel_or_chat");
        notifyRequest.setTargetId("@cniiep_edu_updates");
        notifyRequest.setTemplateLanguage("ru");
        notifyRequest.setMessageFormat("markdown_v2");

        TelegramNotificationRequest.PayloadDetails payload = new TelegramNotificationRequest.PayloadDetails();
        payload.setDocumentId(id.toString());
        payload.setTitle(docOpt.get().getTitle());
        payload.setActionType("запрос_актуализации");
        payload.setAuthorName(role);
        payload.setUpdateSummary("Запрос на актуализацию: " + reason);
        payload.setDirectLink("https://kb.crie.ru/documents/" + id);
        notifyRequest.setPayload(payload);

        String renderedMessage = "⚠️ *Запрос актуализации документа*\n\n" +
                "📝 *Документ:* " + escapeMarkdownV2(docOpt.get().getTitle()) + "\n" +
                "👤 *Заявитель:* " + escapeMarkdownV2(role) + "\n" +
                "❔ *Причина:* " + escapeMarkdownV2(reason);
        notifyRequest.setRenderedMessage(renderedMessage);

        notificationDispatcher.dispatchTelegram(notifyRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    // Response models
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

    public static class DocumentResponseDTO {
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

    public static class DocumentDetailsResponseDTO {
        private DocumentResponseDTO document;
        private List<DocumentVersionResponseDTO> versions;

        public DocumentResponseDTO getDocument() { return document; }
        public void setDocument(DocumentResponseDTO document) { this.document = document; }
        public List<DocumentVersionResponseDTO> getVersions() { return versions; }
        public void setVersions(List<DocumentVersionResponseDTO> versions) { this.versions = versions; }
    }

    public static class DocumentVersionResponseDTO {
        private String id;
        private Integer versionNumber;
        private String status;
        private String changesSummary;
        private String fileUrl;
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Integer getVersionNumber() { return versionNumber; }
        public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getChangesSummary() { return changesSummary; }
        public void setChangesSummary(String changesSummary) { this.changesSummary = changesSummary; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
