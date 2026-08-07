package com.eneik.production.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "allocated_amount", nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "spent_amount", nullable = false)
    private BigDecimal spentAmount;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visibility_rule_id")
    private VisibilityRule visibilityRule;

    public Budget() {}

    public Budget(String departmentName, BigDecimal allocatedAmount, BigDecimal spentAmount, String status, VisibilityRule visibilityRule) {
        this.departmentName = departmentName;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
        this.status = status;
        this.visibilityRule = visibilityRule;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public VisibilityRule getVisibilityRule() {
        return visibilityRule;
    }

    public void setVisibilityRule(VisibilityRule visibilityRule) {
        this.visibilityRule = visibilityRule;
    }
}
