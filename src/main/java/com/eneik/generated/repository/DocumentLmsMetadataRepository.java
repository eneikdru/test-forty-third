package com.eneik.generated.repository;

import com.eneik.generated.model.DocumentLmsMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentLmsMetadataRepository extends JpaRepository<DocumentLmsMetadata, UUID> {
    List<DocumentLmsMetadata> findByDocumentId(UUID documentId);
}
