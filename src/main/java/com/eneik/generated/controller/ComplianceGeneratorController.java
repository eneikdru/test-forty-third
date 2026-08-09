package com.eneik.generated.controller;

import com.eneik.generated.dto.CoverageAuditRequest;
import com.eneik.generated.dto.CoverageAuditResponse;
import com.eneik.generated.dto.TaskPlanRequest;
import com.eneik.generated.dto.TaskPlanResponse;
import com.eneik.generated.service.ComplianceGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceGeneratorController {

    private final ComplianceGeneratorService complianceGeneratorService;

    public ComplianceGeneratorController(ComplianceGeneratorService complianceGeneratorService) {
        this.complianceGeneratorService = complianceGeneratorService;
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

    @PostMapping("/coverage-audit")
    public ResponseEntity<?> auditCoverage(HttpServletRequest httpRequest, @RequestBody CoverageAuditRequest request) {
        String role = extractRole(httpRequest);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/task-plan")
    public ResponseEntity<?> validateTaskPlan(HttpServletRequest httpRequest, @RequestBody TaskPlanRequest request) {
        String role = extractRole(httpRequest);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }
        TaskPlanResponse response = complianceGeneratorService.validateTaskPlan(request);
        if (!response.isValidated()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
