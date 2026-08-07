package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.SchemaTag;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.DocumentVersionRepository;
import com.eneik.generated.repository.SchemaTagRepository;
import com.eneik.generated.service.AnalyticsService;
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

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final SchemaTagRepository schemaTagRepository;
    private final IdProvider idProvider;
    private final TimeProvider timeProvider;
    private final AnalyticsService analyticsService;

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of("Position", "Procedure", "Project", "Other");
    private static final Set<String> ALLOWED_PROGRAMS = Set.of("postgraduate", "residency", "both");
    private static final Set<String> ALLOWED_PROCESSES = Set.of("admission", "certification", "stipends", "practice", "result_tracking", "other");
    private static final Pattern ACADEMIC_YEAR_PATTERN = Pattern.compile("^(infinite|project|бессрочно|проект|\\d{4}-\\d{4}|\\d{4}–\\d{4})$");

    public DocumentController(DocumentRepository documentRepository,
                              DocumentVersionRepository documentVersionRepository,
                              SchemaTagRepository schemaTagRepository,
                              IdProvider idProvider,
                              TimeProvider timeProvider,
                              AnalyticsService analyticsService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.schemaTagRepository = schemaTagRepository;
        this.idProvider = idProvider;
        this.timeProvider = timeProvider;
        this.analyticsService = analyticsService;
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
            @RequestParam(value = "process", required = false) String process) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        List<Document> documents = documentRepository.findAll();
        List<DocumentResponseDTO> responses = documents.stream()
                .filter(doc -> categoryId == null || (doc.getCategory() != null && doc.getCategory().getId().equals(categoryId)))
                .filter(doc -> program == null || program.equalsIgnoreCase(doc.getProgram()))
                .filter(doc -> process == null || process.equalsIgnoreCase(doc.getProcess()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
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
