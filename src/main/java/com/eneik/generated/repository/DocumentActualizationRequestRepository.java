package com.eneik.generated.repository;

import com.eneik.generated.model.DocumentActualizationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentActualizationRequestRepository extends JpaRepository<DocumentActualizationRequest, UUID> {
    List<DocumentActualizationRequest> findByDocumentId(UUID documentId);
}
