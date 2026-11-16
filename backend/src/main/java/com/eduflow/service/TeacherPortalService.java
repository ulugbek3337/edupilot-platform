package com.eduflow.service;

import com.eduflow.dto.AcademicDtos.AttendanceRequest;
import com.eduflow.dto.AcademicDtos.GradeRequest;
import com.eduflow.dto.TeacherPortalDtos.*;
import com.eduflow.entity.*;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.*;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TeacherPortalService {
    private final CenterRepository centers;
    private final TeacherRepository teachers;
    private final UserRepository users;
    private final CenterMembershipRepository memberships;
    private final StudyGroupRepository groups;
    private final LessonRepository lessons;
    private final GroupEnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final AcademicService academic;
    private final PasswordEncoder passwordEncoder;
    private final TenantRlsContext tenantRlsContext;
    private final AuditLogService audit;
    private final SecureRandom random = new SecureRandom();

    private Center center(Long id) {
        if (id == null) throw new EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        Center center = centers.findById(id).orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "center.not-found"));
        if (center.getStatus() != CenterStatus.ACTIVE) throw new EduFlowException(HttpStatus.FORBIDDEN, "tenant.center.inactive");
        tenantRlsContext.apply(id);
        return center;
    }

    @Transactional
    public AccountCredentials createAccount(Long centerId, Long teacherId, String email) {
        Center center = center(centerId);
        Teacher teacher = teachers.findByIdAndCenter_Id(teacherId, centerId)
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "teacher.not-found"));
        if (teacher.getUser() != null) throw new EduFlowException(HttpStatus.CONFLICT, "teacher.account.exists");
        if (email == null || email.isBlank() || email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "teacher.account.email.invalid");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) throw new EduFlowException(HttpStatus.CONFLICT, "auth.email.already-registered");

        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tenantRlsContext.applyUserEmail(normalizedEmail);
        User user = new User(normalizedEmail, passwordEncoder.encode(temporaryPassword), teacher.getFirstName(), teacher.getLastName());
        user.setMustChangePassword(true);
        user = users.save(user);
        memberships.save(new CenterMembership(center, user, Role.TEACHER));
        teacher.setUser(user);
        return new AccountCredentials(teacher.getId(), normalizedEmail, temporaryPassword, true);
    }

    @Transactional(readOnly = true)
    public TeacherProfile me(Long centerId, Long userId) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        return new TeacherProfile(teacher.getId(), teacher.getTeacherCode(), teacher.getFirstName(),
                teacher.getLastName(), teacher.getSpecialization());
    }

    @Transactional(readOnly = true)
    public List<GroupItem> myGroups(Long centerId, Long userId) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        return groups.findAllByCenter_IdAndTeacher_IdAndActiveTrueOrderByNameAsc(centerId, teacher.getId())
                .stream().map(g -> new GroupItem(g.getId(), g.getName(), g.getGroupCode(),
                        g.getCourse().getName(), g.getStartsOn(), g.getEndsOn())).toList();
    }

    @Transactional(readOnly = true)
    public List<LessonItem> lessons(Long centerId, Long userId, Long groupId) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        ownedGroup(centerId, teacher, groupId);
        return lessons.findAllByCenter_IdAndGroup_IdOrderByStartsAtAsc(centerId, groupId).stream()
                .map(l -> new LessonItem(l.getId(), groupId, l.getTitle(), l.getStartsAt(), l.getEndsAt(), l.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentItem> students(Long centerId, Long userId, Long groupId) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        ownedGroup(centerId, teacher, groupId);
        return enrollments.findAllByCenter_IdAndGroup_IdAndActiveTrueOrderByStudent_LastNameAsc(centerId, groupId)
                .stream().map(e -> new StudentItem(e.getStudent().getId(), e.getStudent().getStudentNumber(),
                        e.getStudent().getFirstName(), e.getStudent().getLastName())).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceItem> lessonAttendance(Long centerId, Long userId, Long lessonId) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        Lesson lesson = lessons.findByIdAndCenter_Id(lessonId, centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "lesson.not-found"));
        ownedGroup(centerId, teacher, lesson.getGroup().getId());
        return attendance.findAllByCenter_IdAndLesson_IdOrderByStudent_LastNameAsc(centerId, lessonId).stream()
                .map(a -> new AttendanceItem(a.getId(), lessonId, a.getStudent().getId(),
                        a.getStudent().getFirstName() + " " + a.getStudent().getLastName(),
                        a.getStatus(), a.getNote(), a.getCreatedAt())).toList();
    }

    @Transactional
    public void markAttendance(Long centerId, Long userId, AttendanceRequest request) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        Lesson lesson = lessons.findByIdAndCenter_Id(request.lessonId(), centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "lesson.not-found"));
        ownedGroup(centerId, teacher, lesson.getGroup().getId());
        academic.markAttendance(centerId, request);
        audit.record(centerId, userId, "TEACHER_ATTENDANCE_MARKED", "Lesson",
                request.lessonId(), "Student " + request.studentId());
    }

    @Transactional
    public void addGrade(Long centerId, Long userId, GradeRequest request) {
        center(centerId);
        Teacher teacher = teacher(centerId, userId);
        ownedGroup(centerId, teacher, request.groupId());
        academic.addGrade(centerId, request);
        audit.record(centerId, userId, "TEACHER_GRADE_CREATED", "StudyGroup",
                request.groupId(), "Student " + request.studentId() + "; " + request.title());
    }

    private Teacher teacher(Long centerId, Long userId) {
        return teachers.findByUser_IdAndCenter_Id(userId, centerId)
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .orElseThrow(() -> new EduFlowException(HttpStatus.FORBIDDEN, "teacher.profile.not-linked"));
    }

    private void ownedGroup(Long centerId, Teacher teacher, Long groupId) {
        groups.findByIdAndCenter_IdAndActiveTrue(groupId, centerId)
                .filter(g -> g.getTeacher().getId().equals(teacher.getId()))
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "teacher.group.not-found"));
    }
}
