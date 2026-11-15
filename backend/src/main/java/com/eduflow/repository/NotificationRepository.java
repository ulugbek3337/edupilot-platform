package com.eduflow.repository;
import com.eduflow.entity.Notification; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,Long>{
 List<Notification> findTop100ByCenter_IdAndRecipient_IdOrderByCreatedAtDesc(Long centerId,Long recipientId);
 Optional<Notification> findByIdAndCenter_IdAndRecipient_Id(Long id,Long centerId,Long recipientId);
}
