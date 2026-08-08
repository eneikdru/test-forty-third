package com.eneik.generated.repository;

import com.eneik.generated.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Task t SET t.status = :newStatus, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.status = :expectedOldStatus")
    int updateStatusAtomically(
        @Param("id") UUID id,
        @Param("newStatus") String newStatus,
        @Param("expectedOldStatus") String expectedOldStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Task t SET t.status = :newStatus, t.githubPrState = :githubPrState, t.githubPrMerged = :githubPrMerged, t.updatedAt = :updatedAt WHERE t.id = :id AND t.status = :expectedOldStatus")
    int updateStatusAndPrStateAtomically(
        @Param("id") UUID id,
        @Param("newStatus") String newStatus,
        @Param("expectedOldStatus") String expectedOldStatus,
        @Param("githubPrState") String githubPrState,
        @Param("githubPrMerged") Boolean githubPrMerged,
        @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    long countByStatus(String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Task t SET t.status = :newStatus, t.githubPrNumber = null, t.githubPrState = null, t.githubPrMerged = null, t.updatedAt = :updatedAt WHERE t.status = :expectedOldStatus")
    int updateAllStatusAtomically(
        @Param("expectedOldStatus") String expectedOldStatus,
        @Param("newStatus") String newStatus,
        @Param("updatedAt") java.time.LocalDateTime updatedAt
    );
}
