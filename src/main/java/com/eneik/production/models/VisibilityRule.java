package com.eneik.production.models;

import jakarta.persistence.*;

@Entity
@Table(name = "visibility_rules")
public class VisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String role;

    @Column(name = "allowed_actions", nullable = false)
    private String allowedActions;

    @Column
    private String description;

    public VisibilityRule() {}

    public VisibilityRule(String role, String allowedActions, String description) {
        this.role = role;
        this.allowedActions = allowedActions;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(String allowedActions) {
        this.allowedActions = allowedActions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
