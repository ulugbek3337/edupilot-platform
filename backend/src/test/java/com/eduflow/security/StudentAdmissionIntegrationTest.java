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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:student_admission_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class StudentAdmissionIntegrationTest {
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
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwt;

    @Test
    @Transactional
    void admissionCreatesStudentAccountAndStudentIsRestrictedToOwnPortalAndPassword() throws Exception {
        User admin = activeUser("admission-admin@example.test");
        Center center = centers.save(new Center("Admission Center", "admission-center", CenterStatus.ACTIVE));
        memberships.save(new CenterMembership(center, admin, Role.CENTER_ADMIN));

        Teacher teacher = teachers.save(new Teacher(center, "T-ADM-01", "Olim", "Karimov", null, null, null));
        Course course = courses.save(new Course(center, "C-ADM-01", "English", null));
        StudyGroup group = groups.save(new StudyGroup(center, course, teacher, "G-ADM-01", "English A1", null, null));
        String adminToken = jwt.generateToken(admin, center.getId());

        String response = mvc.perform(post("/api/v1/student-admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentNumber":"ST-ADM-01",
                                  "firstName":"Ali",
                                  "lastName":"Valiyev",
                                  "phone":"+998901234567",
                                  "groupId":%d
                                }
                                """.formatted(group.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupName").value("English A1"))
                .andExpect(jsonPath("$.teacherName").value("Olim Karimov"))
                .andReturn().getResponse().getContentAsString();

        JsonNode credentials = mapper.readTree(response);
        String login = credentials.get("login").asText();
        String temporaryPassword = credentials.get("temporaryPassword").asText();
        assertFalse(login.isBlank());
        assertEquals(14, temporaryPassword.length());

        User studentUser = users.findByEmail(login).orElseThrow();
        assertTrue(studentUser.isStudentAccount());
        assertTrue(studentUser.isMustChangePassword());
        assertTrue(passwordEncoder.matches(temporaryPassword, studentUser.getPasswordHash()));
        assertFalse(studentUser.getPasswordHash().equals(temporaryPassword));

        Student student = students.findByUser_IdAndCenter_Id(studentUser.getId(), center.getId()).orElseThrow();
        assertEquals("ST-ADM-01", student.getStudentNumber());
        assertTrue(enrollments.existsByCenter_IdAndGroup_IdAndStudent_IdAndActiveTrue(
                center.getId(), group.getId(), student.getId()));
        assertTrue(memberships.existsByCenterIdAndUserIdAndRole(center.getId(), studentUser.getId(), Role.STUDENT));

        String studentToken = jwt.generateToken(studentUser, center.getId());
        mvc.perform(get("/api/v1/student-portal/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"NewSecureStudentPassword2026!"}
                                """.formatted(temporaryPassword)))
                .andExpect(status().isNoContent());

        assertFalse(users.findById(studentUser.getId()).orElseThrow().isMustChangePassword());

        mvc.perform(get("/api/v1/student-portal/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("ST-ADM-01"))
                .andExpect(jsonPath("$.groupName").value("English A1"))
                .andExpect(jsonPath("$.teacherName").value("Olim Karimov"));

        mvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isForbidden());
    }

    private User activeUser(String email) {
        User user = new User(email, passwordEncoder.encode("AdminPassword2026!"), "Center", "Admin");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        return users.save(user);
    }
}
