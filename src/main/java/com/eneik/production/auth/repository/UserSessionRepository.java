package com.eneik.production.auth.repository;

import com.eneik.production.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    java.util.Optional<UserSession> findByTokenHash(String tokenHash);
}
