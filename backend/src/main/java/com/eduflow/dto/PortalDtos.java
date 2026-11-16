package com.eduflow.dto;
import jakarta.validation.constraints.NotNull; import java.time.Instant; import java.math.BigDecimal;
public final class PortalDtos {
 private PortalDtos(){}
 public record ParentLinkRequest(@NotNull Long parentUserId,@NotNull Long studentId){}
 public record ParentLinkResponse(Long id,Long parentUserId,String parentEmail,Long studentId,String studentName,boolean active,Instant createdAt){}
 public record ChildResponse(Long id,String studentNumber,String firstName,String lastName){}
 public record NotificationResponse(Long id,String type,String title,String message,Instant createdAt,Instant readAt){}
 public record AuditResponse(Long id,Long actorUserId,String action,String entityType,Long entityId,String details,Instant createdAt){}
 public record ParentGradeResponse(Long id,String title,BigDecimal score,BigDecimal maxScore,String comment,Instant createdAt){}
 public record ParentAttendanceResponse(Long id,Long lessonId,String lessonTitle,String status,String note,Instant createdAt){}
}
