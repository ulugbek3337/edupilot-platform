package com.eduflow.security;
import com.eduflow.entity.*; import com.eduflow.repository.*; import com.fasterxml.jackson.databind.JsonNode; import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.context.ActiveProfiles; import org.springframework.test.context.TestPropertySource; import org.springframework.test.web.servlet.MockMvc; import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestPropertySource(properties={"DB_URL=jdbc:h2:mem:academic_tenant_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","DB_USERNAME=sa","DB_PASSWORD=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.flyway.enabled=false","JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=","JWT_EXPIRATION_MS=3600000","JWT_REFRESH_EXPIRATION_MS=604800000"})
class AcademicTenantIntegrationTest {
 @Autowired private MockMvc mvc; @Autowired private UserRepository users; @Autowired private CenterRepository centers; @Autowired private CenterMembershipRepository memberships; @Autowired private StudentRepository students; @Autowired private JwtService jwt; @Autowired private ObjectMapper mapper;
 @Test @Transactional void centerAdminCanCreateAcademicRecordsWithinOwnTenant() throws Exception {
  User admin=new User("academic-admin@example.test","not-a-real-password-hash","Academic","Admin");admin.setEnabled(true);admin.setStatus(UserStatus.ACTIVE);admin=users.save(admin);
  Center center=centers.save(new Center("Academic Center","academic-center",CenterStatus.ACTIVE));memberships.save(new CenterMembership(center,admin,Role.CENTER_ADMIN));
  Student student=students.save(new Student(center,"ST-AC-1","Ali","Valiyev",null,null,null));String token=jwt.generateToken(admin,center.getId());
  JsonNode teacher=post(token,center.getId(),"/api/v1/teachers","{\"teacherCode\":\"T-01\",\"firstName\":\"Dilshod\",\"lastName\":\"Karimov\"}");
  JsonNode course=post(token,center.getId(),"/api/v1/courses","{\"courseCode\":\"MATH\",\"name\":\"Matematika\"}");
  JsonNode group=post(token,center.getId(),"/api/v1/groups","{\"courseId\":"+course.get("id").asLong()+",\"teacherId\":"+teacher.get("id").asLong()+",\"groupCode\":\"G-01\",\"name\":\"7-A\"}");
  post(token,center.getId(),"/api/v1/group-enrollments","{\"groupId\":"+group.get("id").asLong()+",\"studentId\":"+student.getId()+"}");
  JsonNode lesson=post(token,center.getId(),"/api/v1/lessons","{\"groupId\":"+group.get("id").asLong()+",\"title\":\"Algebra\",\"startsAt\":\"2026-11-16T09:00:00\",\"endsAt\":\"2026-11-16T10:00:00\"}");
  post(token,center.getId(),"/api/v1/attendance","{\"lessonId\":"+lesson.get("id").asLong()+",\"studentId\":"+student.getId()+",\"status\":\"PRESENT\"}");
  post(token,center.getId(),"/api/v1/grades","{\"studentId\":"+student.getId()+",\"groupId\":"+group.get("id").asLong()+",\"title\":\"Nazorat ishi\",\"score\":18,\"maxScore\":20}");
  mvc.perform(get("/api/v1/students/"+student.getId()+"/grades").header("Authorization","Bearer "+token).header("X-Center-Id",center.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/v1/lessons/"+lesson.get("id").asLong()+"/attendance").header("Authorization","Bearer "+token).header("X-Center-Id",center.getId())).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PRESENT"));
 }
 private JsonNode post(String token,Long centerId,String path,String body) throws Exception {String response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).header("Authorization","Bearer "+token).header("X-Center-Id",centerId).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return mapper.readTree(response);}
}
