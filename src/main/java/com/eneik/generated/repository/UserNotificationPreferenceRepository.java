package com.eneik.generated.repository;

import com.eneik.generated.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {
    List<UserNotificationPreference> findByNotifyOnDocumentUpdateTrue();
    java.util.Optional<UserNotificationPreference> findByUserId(UUID userId);
}
