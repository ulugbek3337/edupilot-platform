package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant; import java.time.LocalDateTime;
@Entity @Table(name="lessons",indexes=@Index(name="idx_lessons_center_group_start",columnList="center_id,group_id,starts_at"))
@Getter @Setter @NoArgsConstructor
public class Lesson {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id",nullable=false) private StudyGroup group;
 @Column(nullable=false,length=180) private String title;
 @Column(length=2000) private String description;
 @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
 @Column(name="ends_at",nullable=false) private LocalDateTime endsAt;
 @Column(nullable=false,length=20) private String status="SCHEDULED";
 @Version private long version;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public Lesson(Center c,StudyGroup g,String title,String description,LocalDateTime start,LocalDateTime end){center=c;group=g;this.title=title;this.description=description;startsAt=start;endsAt=end;}
}
