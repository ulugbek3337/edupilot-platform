package com.eduflow.security;

import com.eduflow.entity.*;
import com.eduflow.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:teacher_portal_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_EXPIRATION_MS=3600000",
        "JWT_REFRESH_EXPIRATION_MS=604800000"
})
class TeacherPortalIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private CenterRepository centers;
    @Autowired private CenterMembershipRepository memberships;
    @Autowired private TeacherRepository teachers;
    @Autowired private CourseRepository courses;
    @Autowired private StudyGroupRepository groups;
    @Autowired private StudentRepository students;
    @Autowired private GroupEnrollmentRepository enrollments;
    @Autowired private LessonRepository lessons;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AttendanceRepository attendance;
    @Autowired private JwtService jwt;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private AuditLogRepository auditLogs;

    @Test
    @Transactional
    void provisionedTeacherMustChangePasswordAndCanOnlyReadAssignedGroups() throws Exception {
        User admin = activeUser("teacher-admin@example.test");
        Center center = centers.save(new Center("Teacher Portal Center", "teacher-portal-center", CenterStatus.ACTIVE));
        memberships.save(new CenterMembership(center, admin, Role.CENTER_ADMIN));

        Teacher assignedTeacher = teachers.save(new Teacher(center, "T-PORTAL-01", "Dilshod", "Karimov",
                "dilshod@example.test", null, "Matematika"));
        Teacher otherTeacher = teachers.save(new Teacher(center, "T-PORTAL-02", "Aziza", "Aliyeva",
                "aziza@example.test", null, "Ingliz tili"));
        Course course = courses.save(new Course(center, "C-PORTAL-01", "Matematika", null));
        StudyGroup assignedGroup = groups.save(new StudyGroup(center, course, assignedTeacher,
                "G-PORTAL-01", "Matematika A1", null, null));
        StudyGroup otherGroup = groups.save(new StudyGroup(center, course, otherTeacher,
                "G-PORTAL-02", "Matematika A2", null, null));
        Student assignedStudent = students.save(new Student(center, "ST-PORTAL-01", "Ali", "Valiyev", null, null, null));
        Student otherStudent = students.save(new Student(center, "ST-PORTAL-02", "Malika", "Saidova", null, null, null));
        enrollments.save(new GroupEnrollment(center, assignedGroup, assignedStudent));
        enrollments.save(new GroupEnrollment(center, otherGroup, otherStudent));
        lessons.save(new Lesson(center, assignedGroup, "Algebra", null,
                LocalDateTime.of(2026, 11, 16, 9, 0), LocalDateTime.of(2026, 11, 16, 10, 0)));
        Lesson otherLesson = lessons.save(new Lesson(center, otherGroup, "Geometriya", null,
                LocalDateTime.of(2026, 11, 16, 11, 0), LocalDateTime.of(2026, 11, 16, 12, 0)));

        String adminToken = jwt.generateToken(admin, center.getId());
        String accountResponse = mvc.perform(post("/api/v1/teachers/" + assignedTeacher.getId() + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dilshod@example.test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();

        JsonNode credentials = mapper.readTree(accountResponse);
        String temporaryPassword = credentials.get("temporaryPassword").asText();
        assertFalse(temporaryPassword.isBlank());
        assertTrue(passwordEncoder.matches(temporaryPassword,
                users.findByEmail("dilshod@example.test").orElseThrow().getPasswordHash()));
        assertTrue(memberships.existsByCenterIdAndUserIdAndRole(center.getId(),
                users.findByEmail("dilshod@example.test").orElseThrow().getId(), Role.TEACHER));

        String loginResponse = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dilshod@example.test","password":"%s"}
                                """.formatted(temporaryPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String teacherToken = mapper.readTree(loginResponse).get("accessToken").asText();

        mvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberships[0].role").value("TEACHER"));
        mvc.perform(get("/api/v1/teacher-portal/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"NewTeacherPassword2026!"}
                                """.formatted(temporaryPassword)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/teacher-portal/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherCode").value("T-PORTAL-01"));
        mvc.perform(get("/api/v1/teacher-portal/groups/" + assignedGroup.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentNumber").value("ST-PORTAL-01"));

        mvc.perform(get("/api/v1/teacher-portal/groups/" + otherGroup.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/teacher-portal/lessons/" + otherLesson.getId() + "/attendance")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/teacher-portal/attendance")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lessonId":%d,"studentId":%d,"status":"PRESENT","note":"Test"}
                                """.formatted(lessons.findAllByCenter_IdAndGroup_IdOrderByStartsAtAsc(center.getId(), assignedGroup.getId()).get(0).getId(), assignedStudent.getId())))
                .andExpect(status().isNoContent());
        Long assignedLessonId = lessons.findAllByCenter_IdAndGroup_IdOrderByStartsAtAsc(center.getId(), assignedGroup.getId()).get(0).getId();
        mvc.perform(post("/api/v1/teacher-portal/attendance")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lessonId":%d,"studentId":%d,"status":"LATE","note":"Updated"}
                                """.formatted(assignedLessonId, assignedStudent.getId())))
                .andExpect(status().isNoContent());
        var attendanceRows = attendance.findAllByCenter_IdAndLesson_IdOrderByStudent_LastNameAsc(center.getId(), assignedLessonId);
        assertEquals(1, attendanceRows.size(), "Attendance updates must not create duplicate lesson/student records");
        assertEquals("LATE", attendanceRows.get(0).getStatus());
        assertEquals("Updated", attendanceRows.get(0).getNote());

        mvc.perform(post("/api/v1/teacher-portal/attendance")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lessonId":%d,"studentId":%d,"status":"PRESENT"}
                                """.formatted(otherLesson.getId(), otherStudent.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/teacher-portal/grades")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"groupId":%d,"title":"Algebra test","score":90,"maxScore":100}
                                """.formatted(assignedStudent.getId(), assignedGroup.getId())))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/teacher-portal/grades")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"groupId":%d,"title":"Unauthorized grade","score":90,"maxScore":100}
                                """.formatted(otherStudent.getId(), otherGroup.getId())))
                .andExpect(status().isNotFound());

        var auditActions = auditLogs.findTop200ByCenter_IdOrderByCreatedAtDesc(center.getId()).stream()
                .map(AuditLog::getAction).toList();
        assertTrue(auditActions.contains("TEACHER_ATTENDANCE_MARKED"));
        assertTrue(auditActions.contains("TEACHER_GRADE_CREATED"));
    }


    @Test
    @Transactional
    void deactivatingTeacherRevokesAccountAndRefreshTokens() throws Exception {
        User admin = activeUser("teacher-lifecycle-admin@example.test");
        Center center = centers.save(new Center("Teacher Lifecycle Center", "teacher-lifecycle-center", CenterStatus.ACTIVE));
        memberships.save(new CenterMembership(center, admin, Role.CENTER_ADMIN));
        Teacher teacher = teachers.save(new Teacher(center, "T-LIFECYCLE-01", "Madina", "Rasulova",
                "madina@example.test", null, "Fizika"));

        String adminToken = jwt.generateToken(admin, center.getId());
        String credentialsJson = mvc.perform(post("/api/v1/teachers/" + teacher.getId() + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"madina@example.test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = mapper.readTree(credentialsJson).get("temporaryPassword").asText();
        String loginJson = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"madina@example.test\",\"password\":\"%s\"}", temporaryPassword)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode login = mapper.readTree(loginJson);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();
        User teacherUser = users.findByEmail("madina@example.test").orElseThrow();
        assertTrue(teacherUser.isEnabled());
        assertTrue(memberships.findByCenterIdAndUserId(center.getId(), teacherUser.getId()).isPresent());

        mvc.perform(delete("/api/v1/teachers/" + teacher.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isNoContent());

        assertFalse(users.findById(teacherUser.getId()).orElseThrow().isEnabled());
        assertEquals(MembershipStatus.INACTIVE,
                memberships.findByCenterIdAndUserId(center.getId(), teacherUser.getId()).orElseThrow().getStatus());
        assertTrue(refreshTokens.findByToken(refreshToken).isEmpty());

        mvc.perform(get("/api/v1/teacher-portal/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isUnauthorized());
    }

    private User activeUser(String email) {
        User user = new User(email, passwordEncoder.encode("AdminPassword2026!"), "Center", "Admin");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        return users.save(user);
    }
}
