package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="parent_student_links",uniqueConstraints=@UniqueConstraint(name="uk_parent_student_center",columnNames={"center_id","parent_user_id","student_id"}))
@Getter @Setter @NoArgsConstructor
public class ParentStudentLink {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="parent_user_id",nullable=false) private User parent;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
 @Column(nullable=false) private boolean active=true;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public ParentStudentLink(Center c,User p,Student s){center=c;parent=p;student=s;}
}
