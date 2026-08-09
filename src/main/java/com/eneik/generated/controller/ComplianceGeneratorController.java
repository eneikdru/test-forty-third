package com.eneik.generated.controller;

import com.eneik.generated.dto.CoverageAuditRequest;
import com.eneik.generated.dto.CoverageAuditResponse;
import com.eneik.generated.dto.TaskPlanRequest;
import com.eneik.generated.dto.TaskPlanResponse;
import com.eneik.generated.service.ComplianceGeneratorService;
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

    @PostMapping("/coverage-audit")
    public ResponseEntity<CoverageAuditResponse> auditCoverage(@RequestBody CoverageAuditRequest request) {
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/task-plan")
    public ResponseEntity<TaskPlanResponse> validateTaskPlan(@RequestBody TaskPlanRequest request) {
        TaskPlanResponse response = complianceGeneratorService.validateTaskPlan(request);
        if (!response.isValidated()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
