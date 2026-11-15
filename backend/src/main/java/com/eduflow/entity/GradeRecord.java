package com.eduflow.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter; import org.hibernate.annotations.CreationTimestamp; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="grade_records",indexes=@Index(name="idx_grades_center_student",columnList="center_id,student_id"))
@Getter @Setter @NoArgsConstructor
public class GradeRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="center_id",nullable=false,updatable=false) private Center center;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id",nullable=false) private StudyGroup group;
 @Column(nullable=false,length=180) private String title;
 @Column(nullable=false,precision=6,scale=2) private BigDecimal score;
 @Column(nullable=false,precision=6,scale=2) private BigDecimal maxScore;
 @Column(length=1000) private String comment;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 public GradeRecord(Center c,Student s,StudyGroup g,String title,BigDecimal score,BigDecimal max,String comment){center=c;student=s;group=g;this.title=title;this.score=score;maxScore=max;this.comment=comment;}
}
