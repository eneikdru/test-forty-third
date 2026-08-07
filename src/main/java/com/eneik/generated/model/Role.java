package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_schema_tags",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "schema_tag_id")
    )
    private Set<SchemaTag> schemaTags = new HashSet<>();

    public Role() {}

    public Role(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Set<SchemaTag> getSchemaTags() { return schemaTags; }
    public void setSchemaTags(Set<SchemaTag> schemaTags) { this.schemaTags = schemaTags; }
}
