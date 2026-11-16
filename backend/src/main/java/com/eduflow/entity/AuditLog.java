package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="audit_logs",indexes=@Index(name="idx_audit_center_created",columnList="center_id,created_at"))
@Getter @Setter @NoArgsConstructor
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_user_id") private User actor;
 @Column(nullable=false,length=80) private String action;
 @Column(name="entity_type",nullable=false,length=80) private String entityType;
 @Column(name="entity_id") private Long entityId;
 @Column(length=1500) private String details;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public AuditLog(Center c,User actor,String action,String type,Long entityId,String details){center=c;this.actor=actor;this.action=action;entityType=type;this.entityId=entityId;this.details=details;}
}
