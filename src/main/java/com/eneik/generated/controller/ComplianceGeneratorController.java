package com.eneik.generated.controller;

import com.eneik.generated.service.ComplianceGeneratorService;
import com.eneik.generated.service.TechnicalLeadCompiler.CoverageAudit;
import com.eneik.generated.service.TechnicalLeadCompiler.Specification;
import com.eneik.generated.service.TechnicalLeadCompiler.TaskPlan;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceGeneratorController {

    private final ComplianceGeneratorService complianceGeneratorService;

    public ComplianceGeneratorController(ComplianceGeneratorService complianceGeneratorService) {
        this.complianceGeneratorService = complianceGeneratorService;
    }

    @PostMapping("/coverage-audit")
    public ResponseEntity<?> performCoverageAudit(
            HttpServletRequest request,
            @RequestBody CoverageAuditRequest body) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        boolean isAuthorized = role.equalsIgnoreCase("Administrator") ||
                role.equalsIgnoreCase("ADMINISTRATOR") ||
                role.equalsIgnoreCase("Content Manager") ||
                role.equalsIgnoreCase("CONTENT_MANAGER") ||
                role.equalsIgnoreCase("ContentManager");

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access forbidden for user role: " + role));
        }

        if (body == null || body.getSpecification() == null || body.getAudit() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Specification and audit are required"));
        }

        try {
            Specification spec = new Specification(body.getSpecification().getSections());
            CoverageAudit audit = new CoverageAudit(body.getAudit().getGaps(), body.getAudit().getAddressedSections());

            complianceGeneratorService.validateCoverageAudit(audit, spec);

            CoverageAuditResponse response = new CoverageAuditResponse();
            response.setStatus("VALIDATED");
            response.setAudit(body.getAudit());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/task-plan")
    public ResponseEntity<?> validateTaskPlan(
            HttpServletRequest request,
            @RequestBody TaskPlanValidationRequest body) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        boolean isAuthorized = role.equalsIgnoreCase("Administrator") ||
                role.equalsIgnoreCase("ADMINISTRATOR") ||
                role.equalsIgnoreCase("Content Manager") ||
                role.equalsIgnoreCase("CONTENT_MANAGER") ||
                role.equalsIgnoreCase("ContentManager");

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access forbidden for user role: " + role));
        }

        if (body == null || body.getPlan() == null || body.getSpecification() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Task plan and specification are required"));
        }

        try {
            TaskPlan plan = new TaskPlan(
                    body.getPlan().getTitle(),
                    body.getPlan().getJtbd(),
                    body.getPlan().isCoverageComplete(),
                    body.getPlan().getTocConstraintRef(),
                    body.getPlan().getRequirementRefs()
            );

            Specification spec = new Specification(body.getSpecification().getSections());

            List<String> implemented = body.getImplementedSections() != null ? body.getImplementedSections() : new ArrayList<>();

            List<TaskPlan> existingNetwork = new ArrayList<>();
            if (body.getExistingNetwork() != null) {
                existingNetwork = body.getExistingNetwork().stream()
                        .map(dto -> new TaskPlan(dto.getTitle(), dto.getJtbd(), dto.isCoverageComplete(), dto.getTocConstraintRef(), dto.getRequirementRefs()))
                        .collect(Collectors.toList());
            }

            complianceGeneratorService.validateTaskPlan(plan, spec, implemented, existingNetwork);

            TaskPlanValidationResponse response = new TaskPlanValidationResponse();
            response.setStatus("VALIDATED");
            response.setPlan(body.getPlan());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
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

    // DTO Classes
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse() {}

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class SpecificationDTO {
        private List<String> sections;

        public List<String> getSections() { return sections; }
        public void setSections(List<String> sections) { this.sections = sections; }
    }

    public static class CoverageAuditDTO {
        private List<String> gaps;
        private List<String> addressedSections;

        public List<String> getGaps() { return gaps; }
        public void setGaps(List<String> gaps) { this.gaps = gaps; }
        public List<String> getAddressedSections() { return addressedSections; }
        public void setAddressedSections(List<String> addressedSections) { this.addressedSections = addressedSections; }
    }

    public static class CoverageAuditRequest {
        private SpecificationDTO specification;
        private CoverageAuditDTO audit;

        public SpecificationDTO getSpecification() { return specification; }
        public void setSpecification(SpecificationDTO specification) { this.specification = specification; }
        public CoverageAuditDTO getAudit() { return audit; }
        public void setAudit(CoverageAuditDTO audit) { this.audit = audit; }
    }

    public static class CoverageAuditResponse {
        private String status;
        private CoverageAuditDTO audit;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public CoverageAuditDTO getAudit() { return audit; }
        public void setAudit(CoverageAuditDTO audit) { this.audit = audit; }
    }

    public static class TaskPlanDTO {
        private String title;
        private String jtbd;
        private boolean coverageComplete;
        private String tocConstraintRef;
        private List<String> requirementRefs;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getJtbd() { return jtbd; }
        public void setJtbd(String jtbd) { this.jtbd = jtbd; }
        public boolean isCoverageComplete() { return coverageComplete; }
        public void setCoverageComplete(boolean coverageComplete) { this.coverageComplete = coverageComplete; }
        public String getTocConstraintRef() { return tocConstraintRef; }
        public void setTocConstraintRef(String tocConstraintRef) { this.tocConstraintRef = tocConstraintRef; }
        public List<String> getRequirementRefs() { return requirementRefs; }
        public void setRequirementRefs(List<String> requirementRefs) { this.requirementRefs = requirementRefs; }
    }

    public static class TaskPlanValidationRequest {
        private TaskPlanDTO plan;
        private SpecificationDTO specification;
        private List<String> implementedSections;
        private List<TaskPlanDTO> existingNetwork;

        public TaskPlanDTO getPlan() { return plan; }
        public void setPlan(TaskPlanDTO plan) { this.plan = plan; }
        public SpecificationDTO getSpecification() { return specification; }
        public void setSpecification(SpecificationDTO specification) { this.specification = specification; }
        public List<String> getImplementedSections() { return implementedSections; }
        public void setImplementedSections(List<String> implementedSections) { this.implementedSections = implementedSections; }
        public List<TaskPlanDTO> getExistingNetwork() { return existingNetwork; }
        public void setExistingNetwork(List<TaskPlanDTO> existingNetwork) { this.existingNetwork = existingNetwork; }
    }

    public static class TaskPlanValidationResponse {
        private String status;
        private TaskPlanDTO plan;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public TaskPlanDTO getPlan() { return plan; }
        public void setPlan(TaskPlanDTO plan) { this.plan = plan; }
    }
}
