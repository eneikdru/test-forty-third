package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status;

    @Column(name = "github_pr_number")
    private Integer githubPrNumber;

    @Column(name = "github_pr_state")
    private String githubPrState;

    @Column(name = "github_pr_merged")
    private Boolean githubPrMerged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Task() {}

    public Task(UUID id, String title, String status, Integer githubPrNumber, String githubPrState, Boolean githubPrMerged) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.githubPrNumber = githubPrNumber;
        this.githubPrState = githubPrState;
        this.githubPrMerged = githubPrMerged;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getGithubPrNumber() {
        return githubPrNumber;
    }

    public void setGithubPrNumber(Integer githubPrNumber) {
        this.githubPrNumber = githubPrNumber;
    }

    public String getGithubPrState() {
        return githubPrState;
    }

    public void setGithubPrState(String githubPrState) {
        this.githubPrState = githubPrState;
    }

    public Boolean getGithubPrMerged() {
        return githubPrMerged;
    }

    public void setGithubPrMerged(Boolean githubPrMerged) {
        this.githubPrMerged = githubPrMerged;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
