package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="notifications",indexes=@Index(name="idx_notifications_center_user_created",columnList="center_id,recipient_user_id,created_at"))
@Getter @Setter @NoArgsConstructor
public class Notification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipient_user_id",nullable=false) private User recipient;
 @Column(nullable=false,length=40) private String type;
 @Column(nullable=false,length=180) private String title;
 @Column(nullable=false,length=1500) private String message;
 @Column(name="read_at") private Instant readAt;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public Notification(Center c,User recipient,String type,String title,String message){center=c;this.recipient=recipient;this.type=type;this.title=title;this.message=message;}
}
