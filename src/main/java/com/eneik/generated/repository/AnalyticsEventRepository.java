package com.eneik.generated.repository;

import com.eneik.generated.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    @Query("SELECT e FROM AnalyticsEvent e WHERE " +
           "(:start IS NULL OR e.createdAt >= :start) AND " +
           "(:end IS NULL OR e.createdAt <= :end) " +
           "ORDER BY e.createdAt DESC")
    List<AnalyticsEvent> findEventsInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
