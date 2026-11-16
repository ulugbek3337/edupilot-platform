package com.eduflow.security;
import com.eduflow.entity.*; import com.eduflow.repository.*; import com.fasterxml.jackson.databind.JsonNode; import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.context.ActiveProfiles; import org.springframework.test.context.TestPropertySource; import org.springframework.test.web.servlet.MockMvc; import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; import static org.junit.jupiter.api.Assertions.assertFalse;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestPropertySource(properties={"DB_URL=jdbc:h2:mem:parent_portal_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","DB_USERNAME=sa","DB_PASSWORD=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.flyway.enabled=false","JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=","JWT_EXPIRATION_MS=3600000","JWT_REFRESH_EXPIRATION_MS=604800000"})
class ParentPortalIntegrationTest {
 @Autowired private MockMvc mvc; @Autowired private UserRepository users; @Autowired private CenterRepository centers; @Autowired private CenterMembershipRepository memberships; @Autowired private StudentRepository students; @Autowired private TeacherRepository teachers; @Autowired private CourseRepository courses; @Autowired private StudyGroupRepository groups; @Autowired private LessonRepository lessons; @Autowired private JwtService jwt; @Autowired private ObjectMapper mapper; @Autowired private CenterMembershipService membershipService; @Autowired private ParentStudentLinkRepository parentLinks;
 @Test @Transactional void parentOnlySeesLinkedChildAndReceivesGradeAttendanceNotifications() throws Exception {
  User admin=saveUser("parent-it-admin@example.test");User parent=saveUser("parent-it-user@example.test");User lockedParent=saveUser("parent-it-locked@example.test");lockedParent.setStatus(UserStatus.LOCKED);users.save(lockedParent);
  Center center=centers.save(new Center("Parent Test Center","parent-test-center",CenterStatus.ACTIVE));
  memberships.save(new CenterMembership(center,admin,Role.CENTER_ADMIN));memberships.save(new CenterMembership(center,parent,Role.PARENT));memberships.save(new CenterMembership(center,lockedParent,Role.PARENT));
  assertFalse(membershipService.isActiveMember(lockedParent.getId(),center.getId()),"Locked users must not be treated as active center members.");
  Student student=students.save(new Student(center,"P-ST-01","Aziza","Karimova",null,null,null));
  Student unrelatedStudent=students.save(new Student(center,"P-ST-02","Bekzod","Karimov",null,null,null));
  Teacher teacher=teachers.save(new Teacher(center,"P-T-01","Dilshod","Olimov",null,null,null));
  Course course=courses.save(new Course(center,"P-C-01","Matematika",null));
  StudyGroup group=groups.save(new StudyGroup(center,course,teacher,"P-G-01","5-A",null,null));
  Lesson lesson=lessons.save(new Lesson(center,group,"Kasrlar",null,java.time.LocalDateTime.of(2026,11,16,9,0),java.time.LocalDateTime.of(2026,11,16,10,0)));
  String adminToken=jwt.generateToken(admin,center.getId());String parentToken=jwt.generateToken(parent,center.getId());
  mvc.perform(post("/api/v1/parents/links").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"parentUserId\":"+parent.getId()+",\"studentId\":"+student.getId()+"}"))
   .andExpect(status().isCreated());
  mvc.perform(get("/api/v1/parents/accounts").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].userId").value(parent.getId())).andExpect(jsonPath("$[0].email").value("parent-it-user@example.test"));
  mvc.perform(get("/api/v1/parents/accounts").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/parents/links").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"parentUserId\":"+lockedParent.getId()+",\"studentId\":"+unrelatedStudent.getId()+"}"))
   .andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/parents/links").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"parentUserId\":"+admin.getId()+",\"studentId\":"+unrelatedStudent.getId()+"}"))
   .andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/group-enrollments").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"groupId\":"+group.getId()+",\"studentId\":"+student.getId()+"}"))
   .andExpect(status().isCreated());
  mvc.perform(get("/api/v1/parents/children").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].firstName").value("Aziza"));
  mvc.perform(post("/api/v1/grades").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"studentId\":"+student.getId()+",\"groupId\":"+group.getId()+",\"title\":\"Test\",\"score\":9,\"maxScore\":10}"))
   .andExpect(status().isCreated());
  mvc.perform(post("/api/v1/attendance").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON)
    .content("{\"lessonId\":"+lesson.getId()+",\"studentId\":"+student.getId()+",\"status\":\"PRESENT\"}"))
   .andExpect(status().isCreated());
  mvc.perform(get("/api/v1/parents/children/"+student.getId()+"/grades").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/v1/parents/children/"+student.getId()+"/attendance").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/v1/parents/children/"+unrelatedStudent.getId()+"/grades").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/parents/children/"+unrelatedStudent.getId()+"/attendance").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/notifications").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  mvc.perform(get("/api/v1/audit-logs").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
  mvc.perform(get("/api/v1/audit-logs").param("action","PARENT_LINK_CREATED").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].action").value("PARENT_LINK_CREATED"));
  mvc.perform(get("/api/v1/audit-logs").param("from","2026-11-17T00:00:00Z").param("to","2026-11-16T00:00:00Z").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/audit-logs").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isForbidden());
  ParentStudentLink oldLink=parentLinks.findAllByCenter_IdAndParent_IdAndActiveTrueOrderByStudent_LastNameAsc(center.getId(),parent.getId()).get(0);
  oldLink.setActive(false); parentLinks.save(oldLink);
  mvc.perform(post("/api/v1/parents/links").header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"parentUserId\":"+parent.getId()+",\"studentId\":"+student.getId()+"}"))
   .andExpect(status().isCreated());
  org.junit.jupiter.api.Assertions.assertTrue(parentLinks.findByCenter_IdAndParent_IdAndStudent_IdAndActiveTrue(center.getId(),parent.getId(),student.getId()).isPresent(),"An inactive parent link should be reactivated instead of violating the unique constraint.");
  Long activeLinkId=parentLinks.findByCenter_IdAndParent_IdAndStudent_IdAndActiveTrue(center.getId(),parent.getId(),student.getId()).orElseThrow().getId();
  mvc.perform(delete("/api/v1/parents/links/"+activeLinkId).header("Authorization","Bearer "+adminToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/parents/children").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/v1/parents/children/"+student.getId()+"/grades").header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isNotFound());
  mvc.perform(delete("/api/v1/parents/links/"+activeLinkId).header("Authorization","Bearer "+parentToken).header("X-Center-Id",center.getId()))
   .andExpect(status().isForbidden());
 }
 private User saveUser(String email){User u=new User(email,"not-a-real-password-hash","Integration","User");u.setEnabled(true);u.setStatus(UserStatus.ACTIVE);return users.save(u);}
}
