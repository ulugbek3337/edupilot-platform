package com.eduflow.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public final class TeacherPortalDtos {
    private TeacherPortalDtos() {}
    public record AccountRequest(String email) {}
    public record AccountCredentials(Long teacherId, String email, String temporaryPassword, boolean mustChangePassword) {}
    public record TeacherProfile(Long id, String teacherCode, String firstName, String lastName, String specialization) {}
    public record GroupItem(Long id, String name, String groupCode, String courseName, LocalDate startsOn, LocalDate endsOn) {}
    public record LessonItem(Long id, Long groupId, String title, LocalDateTime startsAt, LocalDateTime endsAt, String status) {}
    public record StudentItem(Long id, String studentNumber, String firstName, String lastName) {}
    public record AttendanceItem(Long id, Long lessonId, Long studentId, String studentName, String status, String note, Instant createdAt) {}
    public record GradeItem(Long id, Long studentId, Long groupId, String title, BigDecimal score, BigDecimal maxScore, String comment, Instant createdAt) {}
    public record Roster(List<StudentItem> students) {}
}
