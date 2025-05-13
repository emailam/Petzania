package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find events that need to be retried
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < 3 ORDER BY e.createdAt")
    List<OutboxEvent> findEventsForRetry();

    /**
     * Find events by entity ID and type
     */
    List<OutboxEvent> findByEntityIdAndEntityType(String entityId, String entityType);

    /**
     * Find events that haven't been processed yet for a specific entity
     */
    List<OutboxEvent> findByEntityIdAndEntityTypeAndProcessed(String entityId, String entityType, boolean processed);

    /**
     * Find old events that have been processed
     */
    List<OutboxEvent> findByProcessedAndProcessedAtBefore(boolean processed, LocalDateTime before);
}