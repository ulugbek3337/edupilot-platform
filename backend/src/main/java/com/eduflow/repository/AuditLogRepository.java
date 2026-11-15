package com.eduflow.repository;
import com.eduflow.entity.AuditLog; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>{
 List<AuditLog> findTop200ByCenter_IdOrderByCreatedAtDesc(Long centerId);
}
