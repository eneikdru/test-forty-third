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

    @Query("SELECT d FROM Document d WHERE " +
           "(:programList IS NULL OR d.program IN :programList) AND " +
           "(:docType IS NULL OR d.documentType = :docType) AND " +
           "(:educationLevel IS NULL OR d.educationLevel = :educationLevel) AND " +
           "(:updateDateStart IS NULL OR d.updatedAt >= :updateDateStart) AND " +
           "(:updateDateEnd IS NULL OR d.updatedAt <= :updateDateEnd)")
    List<Document> findWithFilters(
        @Param("programList") List<String> programList,
        @Param("docType") com.eneik.generated.model.DocumentType docType,
        @Param("educationLevel") String educationLevel,
        @Param("updateDateStart") java.time.LocalDateTime updateDateStart,
        @Param("updateDateEnd") java.time.LocalDateTime updateDateEnd
    );
}
