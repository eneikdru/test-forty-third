package com.eneik.generated.repository;

import com.eneik.generated.model.SchemaTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SchemaTagRepository extends JpaRepository<SchemaTag, UUID> {
    Optional<SchemaTag> findByName(String name);
}
