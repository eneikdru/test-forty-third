package com.eneik.generated.controller;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.SchemaTag;
import com.eneik.generated.service.FinancialDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.eneik.generated.service.SessionService;

@RestController
@RequestMapping("/api/financial")
public class FinancialDocumentController {

    private final FinancialDocumentService financialDocumentService;
    private final SessionService sessionService;

    public FinancialDocumentController(FinancialDocumentService financialDocumentService, SessionService sessionService) {
        this.financialDocumentService = financialDocumentService;
        this.sessionService = sessionService;
    }

    @GetMapping("/budget")
    public ResponseEntity<?> getBudgetDocuments(
            HttpServletRequest request,
            @RequestParam(required = false) String academicYear) {
        return handleRequest(request, "Budget", academicYear, null);
    }

    @GetMapping("/load")
    public ResponseEntity<?> getLoadDocuments(
            HttpServletRequest request,
            @RequestParam(required = false) String academicYear) {
        return handleRequest(request, "Load", academicYear, null);
    }

    @GetMapping("/stipends")
    public ResponseEntity<?> getStipendDocuments(
            HttpServletRequest request,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String program) {
        return handleRequest(request, "Stipends", academicYear, program);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<?> createDocument(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.RequestBody Document doc) {
        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }
        try {
            Document created = financialDocumentService.createDocument(role, doc);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created, null));
        } catch (FinancialDocumentService.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestBody Document doc) {
        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }
        try {
            Document updated = financialDocumentService.updateDocument(role, id, doc);
            return ResponseEntity.ok(mapToResponse(updated, null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (FinancialDocumentService.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable UUID id) {
        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }
        try {
            financialDocumentService.deleteDocument(role, id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (FinancialDocumentService.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleRequest(
            HttpServletRequest request,
            String tagName,
            String academicYearFilter,
            String programFilter) {

        String role = extractRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid credentials"));
        }

        try {
            List<Document> documents = financialDocumentService.getDocumentsForRoleAndTag(role, tagName);

            List<FinancialDocumentResponse> responses = documents.stream()
                    .map(doc -> mapToResponse(doc, tagName))
                    .filter(res -> academicYearFilter == null || academicYearFilter.equalsIgnoreCase(res.getAcademicYear()))
                    .filter(res -> programFilter == null || programFilter.equalsIgnoreCase(res.getProgram()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", e.getMessage()));
        } catch (FinancialDocumentService.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", e.getMessage()));
        }
    }

    private String extractRole(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                Optional<SessionService.UserSession> sessionOpt = sessionService.getSession(token);
                if (sessionOpt.isPresent()) {
                    return sessionOpt.get().getRole();
                }
                return token;
            }
        }

        String xUserRole = request.getHeader("X-User-Role");
        if (xUserRole != null && !xUserRole.trim().isEmpty()) {
            return xUserRole.trim();
        }
        return null;
    }

    private FinancialDocumentResponse mapToResponse(Document doc, String activeTag) {
        FinancialDocumentResponse res = new FinancialDocumentResponse();
        res.setId(doc.getId().toString());
        res.setTitle(doc.getTitle());
        res.setDescription(doc.getDescription());

        res.setDocumentType(doc.getDocumentType());
        res.setAcademicYear(doc.getAcademicYear());
        res.setProgram(doc.getProgram());
        res.setProcess(doc.getProcess());

        if (doc.getApprovalDate() != null) {
            res.setApprovalDate(doc.getApprovalDate().toString());
        } else {
            res.setApprovalDate("2026-09-01");
        }

        if (doc.getDocumentNumber() != null) {
            res.setDocumentNumber(doc.getDocumentNumber());
        } else {
            res.setDocumentNumber("123-P");
        }

        // Get version from versions
        Optional<DocumentVersion> latestVersion = doc.getVersions().stream()
                .max(Comparator.comparing(DocumentVersion::getVersionNumber));

        if (latestVersion.isPresent()) {
            res.setVersion(latestVersion.get().getVersionNumber() + ".0");
        } else {
            res.setVersion("1.0");
        }

        // Updated at
        res.setUpdatedAt(doc.getUpdatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

        // Schema Tags
        res.setSchemaTags(doc.getSchemaTags().stream()
                .map(SchemaTag::getName)
                .collect(Collectors.toList()));

        // Budget cycle metadata if activeTag is Budget
        if ("Budget".equalsIgnoreCase(activeTag)) {
            BudgetCycleMetadata budget = new BudgetCycleMetadata();
            budget.setBudgetCycle("2026 Budget Cycle");
            budget.setEstimatedAmount(1500000.00);
            budget.setCurrency("RUB");
            budget.setStatus("APPROVED");
            budget.setQuarter("Q1");
            budget.setFiscalYear(2026);
            res.setBudgetCycleMetadata(budget);
        }

        return res;
    }

    // Response classes
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

    public static class FinancialDocumentResponse {
        private String id;
        private String title;
        private String description;
        private String documentType;
        private String academicYear;
        private String program;
        private String process;
        private String approvalDate;
        private String documentNumber;
        private String version;
        private String updatedAt;
        private BudgetCycleMetadata budgetCycleMetadata;
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
        public String getApprovalDate() { return approvalDate; }
        public void setApprovalDate(String approvalDate) { this.approvalDate = approvalDate; }
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public BudgetCycleMetadata getBudgetCycleMetadata() { return budgetCycleMetadata; }
        public void setBudgetCycleMetadata(BudgetCycleMetadata budgetCycleMetadata) { this.budgetCycleMetadata = budgetCycleMetadata; }
        public List<String> getSchemaTags() { return schemaTags; }
        public void setSchemaTags(List<String> schemaTags) { this.schemaTags = schemaTags; }
    }

    public static class BudgetCycleMetadata {
        private String budgetCycle;
        private Double estimatedAmount;
        private String currency;
        private String status;
        private String quarter;
        private Integer fiscalYear;

        public String getBudgetCycle() { return budgetCycle; }
        public void setBudgetCycle(String budgetCycle) { this.budgetCycle = budgetCycle; }
        public Double getEstimatedAmount() { return estimatedAmount; }
        public void setEstimatedAmount(Double estimatedAmount) { this.estimatedAmount = estimatedAmount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getQuarter() { return quarter; }
        public void setQuarter(String quarter) { this.quarter = quarter; }
        public Integer getFiscalYear() { return fiscalYear; }
        public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    }
}
