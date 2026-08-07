package com.eneik.generated.repository;

import com.eneik.generated.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT DISTINCT d FROM Document d JOIN d.schemaTags t WHERE t.id IN :tagIds")
    List<Document> findBySchemaTagsIn(@Param("tagIds") List<UUID> tagIds);
}
