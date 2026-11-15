package com.eduflow.entity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
@Entity @Table(name="teachers",uniqueConstraints=@UniqueConstraint(name="uk_teachers_center_code",columnNames={"center_id","teacher_code"}))
@Getter @Setter @NoArgsConstructor
public class Teacher {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @Column(name="teacher_code",nullable=false,length=50) private String teacherCode;
 @Column(name="first_name",nullable=false,length=100) private String firstName;
 @Column(name="last_name",nullable=false,length=100) private String lastName;
 @Column(length=255) private String email; @Column(length=30) private String phone;
 @Column(length=120) private String specialization;
 @Column(nullable=false,length=20) private String status="ACTIVE";
 @Version private long version;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @UpdateTimestamp @Column(name="updated_at",nullable=false) private Instant updatedAt;
 public Teacher(Center c,String code,String first,String last,String email,String phone,String specialization){center=c;teacherCode=code;firstName=first;lastName=last;this.email=email;this.phone=phone;this.specialization=specialization;}
}
