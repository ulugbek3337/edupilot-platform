package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant; import java.time.LocalDate;
@Entity @Table(name="study_groups",uniqueConstraints=@UniqueConstraint(name="uk_groups_center_code",columnNames={"center_id","group_code"}))
@Getter @Setter @NoArgsConstructor
public class StudyGroup {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id",nullable=false) private Course course;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="teacher_id",nullable=false) private Teacher teacher;
 @Column(name="group_code",nullable=false,length=50) private String groupCode;
 @Column(nullable=false,length=150) private String name;
 @Column(name="starts_on") private LocalDate startsOn; @Column(name="ends_on") private LocalDate endsOn;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public StudyGroup(Center c,Course course,Teacher teacher,String code,String name,LocalDate starts,LocalDate ends){center=c;this.course=course;this.teacher=teacher;groupCode=code;this.name=name;startsOn=starts;endsOn=ends;}
}
