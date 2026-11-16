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
            select a from AuditLog a left join a.actor actor
            where a.center.id = :centerId
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:actorUserId is null or actor.id = :actorUserId)
              and (:fromDate is null or a.createdAt >= :fromDate)
              and (:toDate is null or a.createdAt < :toDate)
            order by a.createdAt desc
            """)
    List<AuditLog> searchByCenter(
            @Param("centerId") Long centerId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorUserId") Long actorUserId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}
