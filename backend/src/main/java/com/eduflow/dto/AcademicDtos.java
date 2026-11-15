package com.eduflow.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*;
public final class AcademicDtos {
 private AcademicDtos(){}
 public record TeacherRequest(@NotBlank @Size(max=50) String teacherCode,@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,@Email @Size(max=255) String email,@Size(max=30) String phone,@Size(max=120) String specialization){}
 public record CourseRequest(@NotBlank @Size(max=50) String courseCode,@NotBlank @Size(max=150) String name,@Size(max=2000) String description){}
 public record GroupRequest(@NotNull Long courseId,@NotNull Long teacherId,@NotBlank @Size(max=50) String groupCode,@NotBlank @Size(max=150) String name,LocalDate startsOn,LocalDate endsOn){}
 public record LessonRequest(@NotNull Long groupId,@NotBlank @Size(max=180) String title,@Size(max=2000) String description,@NotNull LocalDateTime startsAt,@NotNull LocalDateTime endsAt){}
 public record AttendanceRequest(@NotNull Long lessonId,@NotNull Long studentId,@NotBlank String status,@Size(max=1000) String note){}
 public record EnrollmentRequest(@NotNull Long groupId,@NotNull Long studentId){}
 public record EnrollmentResponse(Long id,Long groupId,Long studentId,String studentNumber,String firstName,String lastName){}
 public record GradeRequest(@NotNull Long studentId,@NotNull Long groupId,@NotBlank @Size(max=180) String title,@NotNull @DecimalMin("0.00") BigDecimal score,@NotNull @DecimalMin("0.01") BigDecimal maxScore,@Size(max=1000) String comment){}
 public record ItemResponse(Long id,String code,String name,String status,Instant createdAt){}
 public record LessonResponse(Long id,Long groupId,String title,LocalDateTime startsAt,LocalDateTime endsAt,String status){}
 public record AttendanceResponse(Long id,Long lessonId,Long studentId,String status,String note){}
 public record GradeResponse(Long id,Long studentId,Long groupId,String title,BigDecimal score,BigDecimal maxScore,String comment,Instant createdAt){}
}
