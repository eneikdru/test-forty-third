package com.eneik.generated.dto;

import java.util.List;

public class CoverageAuditRequest {
    private List<String> specifications;
    private List<String> addressedRequirementIds;

    public CoverageAuditRequest() {}

    public CoverageAuditRequest(List<String> specifications, List<String> addressedRequirementIds) {
        this.specifications = specifications;
        this.addressedRequirementIds = addressedRequirementIds;
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
}
