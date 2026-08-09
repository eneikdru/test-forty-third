package com.eneik.generated.dto;

import java.util.List;

public class CoverageAuditRequest {
    private List<String> specifications;
    private List<String> addressedRequirementIds;
    private List<String> changedFiles;

    public CoverageAuditRequest() {}

    public CoverageAuditRequest(List<String> specifications, List<String> addressedRequirementIds) {
        this.specifications = specifications;
        this.addressedRequirementIds = addressedRequirementIds;
    }

    public CoverageAuditRequest(List<String> specifications, List<String> addressedRequirementIds, List<String> changedFiles) {
        this.specifications = specifications;
        this.addressedRequirementIds = addressedRequirementIds;
        this.changedFiles = changedFiles;
    }

    public List<String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<String> specifications) {
        this.specifications = specifications;
    }

    public List<String> getAddressedRequirementIds() {
        return addressedRequirementIds;
    }

    public void setAddressedRequirementIds(List<String> addressedRequirementIds) {
        this.addressedRequirementIds = addressedRequirementIds;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = changedFiles;
    }
}
