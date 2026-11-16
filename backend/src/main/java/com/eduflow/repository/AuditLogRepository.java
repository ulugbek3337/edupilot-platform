package com.eduflow.repository;

import com.eduflow.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            select a from AuditLog a
            where a.center.id = :centerId
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:actorUserId is null or a.actor.id = :actorUserId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            order by a.createdAt desc
            """)
    List<AuditLog> searchByCenter(
            @Param("centerId") Long centerId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorUserId") Long actorUserId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
