package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="attendance_records",uniqueConstraints=@UniqueConstraint(name="uk_attendance_lesson_student",columnNames={"lesson_id","student_id"}))
@Getter @Setter @NoArgsConstructor
public class AttendanceRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="lesson_id",nullable=false) private Lesson lesson;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
 @Column(nullable=false,length=20) private String status;
 @Column(length=1000) private String note;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public AttendanceRecord(Center c,Lesson l,Student s,String status,String note){center=c;lesson=l;student=s;this.status=status;this.note=note;}
}
