package com.eneik.generated.controller;

import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.UserRoleRepository;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileDownloadController.class);

    private final UserRoleRepository userRoleRepository;

    private static final java.util.Set<String> ALLOWED_ROLES = java.util.Set.of(
        "administrator", "content_manager", "content manager", "contentmanager",
        "teacher", "student", "economist", "postgraduate", "resident", "hr"
    );

    public FileDownloadController(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
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

        // Strict allowed roles validation to prevent arbitrary client-provided role bypass
        if (!ALLOWED_ROLES.contains(role.trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access forbidden for user role: " + role));
        }

        // Database cross-verification if user ID is supplied to prevent client-header spoofing
        String xUserId = request.getHeader("X-User-Id");
        if (xUserId != null && !xUserId.trim().isEmpty()) {
            try {
                UUID userId = UUID.fromString(xUserId.trim());
                List<UserRole> dbUserRoles = userRoleRepository.findByUserId(userId);
                boolean hasMatchedRole = dbUserRoles.stream()
                        .anyMatch(ur -> ur.getRoleName().equalsIgnoreCase(role));
                if (!hasMatchedRole && !dbUserRoles.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("ACCESS_DENIED", "User ID does not have the specified role in the database"));
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid UUID format
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
