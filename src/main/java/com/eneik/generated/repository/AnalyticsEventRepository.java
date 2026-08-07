package com.eneik.generated.repository;

import com.eneik.generated.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    List<AnalyticsEvent> findByEventType(String eventType);
    List<AnalyticsEvent> findByDocumentId(UUID documentId);
}
