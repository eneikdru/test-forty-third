package com.eneik.generated.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_name"})
})
public class UserRole {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    public UserRole() {}

    public UserRole(UUID id, UUID userId, String roleName) {
        this.id = id;
        this.userId = userId;
        this.roleName = roleName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
