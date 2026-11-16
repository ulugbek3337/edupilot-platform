package com.eduflow.security;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.Student;
import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
import com.eduflow.repository.CenterMembershipRepository;
import com.eduflow.repository.CenterRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "DB_URL=jdbc:h2:mem:academic_tenant_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class AcademicTenantIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CenterRepository centers;
    @Autowired private CenterMembershipRepository memberships;
    @Autowired private StudentRepository students;
    @Autowired private JwtService jwt;
    @Autowired private ObjectMapper mapper;

    @Test
    @Transactional
    void centerAdminCanCreateAcademicRecordsWithinOwnTenant() throws Exception {
        User admin = new User("academic-admin@example.test", "not-a-real-password-hash", "Academic", "Admin");
        admin.setEnabled(true);
        admin.setStatus(UserStatus.ACTIVE);
        admin = users.save(admin);

        Center center = centers.save(new Center("Academic Center", "academic-center", CenterStatus.ACTIVE));
        memberships.save(new CenterMembership(center, admin, Role.CENTER_ADMIN));
        Student student = students.save(new Student(center, "ST-AC-1", "Ali", "Valiyev", null, null, null));
        String token = jwt.generateToken(admin, center.getId());

        JsonNode teacher = createRecord(token, center.getId(), "/api/v1/teachers", """
            {"teacherCode":"T-01","firstName":"Dilshod","lastName":"Karimov"}
            """);
        JsonNode course = createRecord(token, center.getId(), "/api/v1/courses", """
            {"courseCode":"MATH","name":"Matematika"}
            """);
        JsonNode group = createRecord(token, center.getId(), "/api/v1/groups", """
            {"courseId":%d,"teacherId":%d,"groupCode":"G-01","name":"7-A"}
            """.formatted(course.get("id").asLong(), teacher.get("id").asLong()));
        createRecord(token, center.getId(), "/api/v1/group-enrollments", """
            {"groupId":%d,"studentId":%d}
            """.formatted(group.get("id").asLong(), student.getId()));
        JsonNode lesson = createRecord(token, center.getId(), "/api/v1/lessons", """
            {"groupId":%d,"title":"Algebra","startsAt":"2026-11-16T09:00:00","endsAt":"2026-11-16T10:00:00"}
            """.formatted(group.get("id").asLong()));
        createRecord(token, center.getId(), "/api/v1/attendance", """
            {"lessonId":%d,"studentId":%d,"status":"PRESENT"}
            """.formatted(lesson.get("id").asLong(), student.getId()));
        createRecord(token, center.getId(), "/api/v1/grades", """
            {"studentId":%d,"groupId":%d,"title":"Nazorat ishi","score":18,"maxScore":20}
            """.formatted(student.getId(), group.get("id").asLong()));

        mvc.perform(get("/api/v1/students/" + student.getId() + "/grades")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/v1/lessons/" + lesson.get("id").asLong() + "/attendance")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    @Transactional
    void teacherCannotCreateAdminManagedAcademicResources() throws Exception {
        User teacher = new User("academic-teacher@example.test", "not-a-real-password-hash", "Academic", "Teacher");
        teacher.setEnabled(true);
        teacher.setStatus(UserStatus.ACTIVE);
        teacher = users.save(teacher);

        Center center = centers.save(new Center("Teacher Center", "teacher-center", CenterStatus.ACTIVE));
        memberships.save(new CenterMembership(center, teacher, Role.TEACHER));
        String token = jwt.generateToken(teacher, center.getId());

        mvc.perform(get("/api/v1/teachers")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId()))
            .andExpect(status().isOk());

        mvc.perform(post("/api/v1/teachers")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"teacherCode":"T-DENIED","firstName":"No","lastName":"Access"}
                    """))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/courses")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseCode":"C-DENIED","name":"Forbidden course"}
                    """))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", center.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseId":1,"teacherId":1,"groupCode":"G-DENIED","name":"Forbidden group"}
                    """))
            .andExpect(status().isForbidden());
    }

    private JsonNode createRecord(String token, Long centerId, String path, String body) throws Exception {
        String response = mvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .header("X-Center-Id", centerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return mapper.readTree(response);
    }
}
