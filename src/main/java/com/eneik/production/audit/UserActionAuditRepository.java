package com.eneik.production.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserActionAuditRepository extends JpaRepository<UserActionAudit, UUID> {
}
