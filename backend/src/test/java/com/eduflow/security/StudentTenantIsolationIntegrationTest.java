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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:student_tenant_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class StudentTenantIsolationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CenterRepository centerRepository;
    @Autowired private CenterMembershipRepository membershipRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private JwtService jwtService;

    @Test
    @Transactional
    void teacherCanCreateAndListStudentsOnlyInSelectedCenter() throws Exception {
        User teacher = activeUser("teacher-students@example.test");
        Center center = centerRepository.save(new Center("Teacher Center", "teacher-students", CenterStatus.ACTIVE));
        membershipRepository.save(new CenterMembership(center, teacher, Role.TEACHER));
        String token = jwtService.generateToken(teacher, center.getId());

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Center-Id", center.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentNumber":"ST-001","firstName":"Ali","lastName":"Valiyev"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentNumber").value("ST-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Center-Id", center.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentNumber").value("ST-001"));
    }

    @Test
    @Transactional
    void userCannotReadStudentBelongingToAnotherCenter() throws Exception {
        User teacher = activeUser("teacher-foreign@example.test");
        User foreignOwner = activeUser("foreign-owner@example.test");
        Center ownCenter = centerRepository.save(new Center("Own Center", "student-own", CenterStatus.ACTIVE));
        Center foreignCenter = centerRepository.save(new Center("Foreign Center", "student-foreign", CenterStatus.ACTIVE));
        membershipRepository.save(new CenterMembership(ownCenter, teacher, Role.TEACHER));
        membershipRepository.save(new CenterMembership(foreignCenter, foreignOwner, Role.CENTER_ADMIN));
        Student foreignStudent = studentRepository.save(new Student(
                foreignCenter, "FOREIGN-001", "Foreign", "Student", null, null, null));
        String token = jwtService.generateToken(teacher, ownCenter.getId());

        mockMvc.perform(get("/api/v1/students/" + foreignStudent.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Center-Id", ownCenter.getId()))
                .andExpect(status().isNotFound());
    }

    private User activeUser(String email) {
        User user = new User(email, "not-a-real-password-hash", "Integration", "Test");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
