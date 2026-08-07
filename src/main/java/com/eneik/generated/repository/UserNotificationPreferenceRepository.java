package com.eneik.generated.repository;

import com.eneik.generated.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {
    List<UserNotificationPreference> findByNotifyOnDocumentUpdateTrue();
    Optional<UserNotificationPreference> findByUserId(UUID userId);
}
