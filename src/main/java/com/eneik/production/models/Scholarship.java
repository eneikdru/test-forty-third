package com.eneik.production.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "scholarships")
public class Scholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visibility_rule_id")
    private VisibilityRule visibilityRule;

    public Scholarship() {}

    public Scholarship(String studentName, BigDecimal amount, String type, String status, VisibilityRule visibilityRule) {
        this.studentName = studentName;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.visibilityRule = visibilityRule;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
