package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="courses",uniqueConstraints=@UniqueConstraint(name="uk_courses_center_code",columnNames={"center_id","course_code"}))
@Getter @Setter @NoArgsConstructor
public class Course {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @Column(name="course_code",nullable=false,length=50) private String courseCode;
 @Column(nullable=false,length=150) private String name;
 @Column(length=2000) private String description;
 @Column(nullable=false) private boolean active=true;
 @Version private long version;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public Course(Center c,String code,String name,String description){center=c;courseCode=code;this.name=name;this.description=description;}
}
