package com.eneik.generated.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Optional;
import java.util.Set;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.RoleRepository;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.Role;
import com.eneik.generated.model.SchemaTag;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileDownloadController.class);

    private final DocumentRepository documentRepository;
    private final RoleRepository roleRepository;

    public FileDownloadController(DocumentRepository documentRepository, RoleRepository roleRepository) {
        this.documentRepository = documentRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/{documentId}/v{versionNumber}/{filename}")
    public ResponseEntity<?> downloadFile(
            HttpServletRequest request,
            @PathVariable("documentId") UUID documentId,
            @PathVariable("versionNumber") Integer versionNumber,
            @PathVariable("filename") String filename) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "File not found"));
        }

        Document document = docOpt.get();
        Set<SchemaTag> docTags = document.getSchemaTags();
        if (docTags != null && !docTags.isEmpty()) {
            Optional<Role> roleOpt = roleRepository.findByName(role);
            if (roleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("ACCESS_DENIED", "Access denied: unauthorized role"));
            }
            Role roleEntity = roleOpt.get();
            Set<SchemaTag> roleTags = roleEntity.getSchemaTags();
            boolean hasAccess = false;
            for (SchemaTag docTag : docTags) {
                if (roleTags.stream().anyMatch(rt -> rt.getId().equals(docTag.getId()))) {
                    hasAccess = true;
                    break;
                }
            }
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("ACCESS_DENIED", "Access denied: unauthorized role"));
            }
        }

        // Sanitize the filename to prevent directory traversal
        String sanitizedFilename = Paths.get(filename).getFileName().toString();
        if (sanitizedFilename == null || sanitizedFilename.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid filename provided"));
        }

        // Locate and read the file safely
        Path baseDir = Paths.get("data", "uploads").toAbsolutePath().normalize();
        Path targetPath = Paths.get("data", "uploads", documentId.toString(), "v" + versionNumber, sanitizedFilename).toAbsolutePath().normalize();

        // Path Traversal check: ensure targetPath is within baseDir
        if (!targetPath.startsWith(baseDir)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("FORBIDDEN", "Access denied"));
        }

        if (!Files.exists(targetPath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "File not found"));
        }

        // Determine Content-Type
        String contentType = request.getServletContext().getMimeType(targetPath.toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Stream file cleanly via FileSystemResource to prevent OOM
        FileSystemResource resource = new FileSystemResource(targetPath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedFilename + "\"")
                .body(resource);
    }

    private String extractRole(HttpServletRequest request) {
        Object validatedRole = request.getAttribute("X-User-Role");
        if (validatedRole != null) {
            return (String) validatedRole;
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Session-Invalid"))) {
            return null;
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Allow-Fallback"))) {
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
        }
        return null;
    }

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
