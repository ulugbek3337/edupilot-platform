package com.eduflow.security;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class PostgresTenantRlsIntegrationTest {
 @Container
 static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
  .withDatabaseName("edupilot_rls").withUsername("postgres").withPassword("postgres");
 static JdbcTemplate admin;
 static JdbcTemplate app;
 static TransactionTemplate appTransaction;

 @BeforeAll static void migrateAndSeed() {
  DataSource adminDs=dataSource("postgres","postgres");
  admin=new JdbcTemplate(adminDs);
  Flyway.configure().dataSource(postgres.getJdbcUrl(),"postgres","postgres")
   .locations("classpath:db/migration").load().migrate();
  admin.execute("CREATE ROLE edupilot_app LOGIN PASSWORD 'app-password'");
  admin.execute("GRANT USAGE ON SCHEMA public TO edupilot_app");
  admin.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO edupilot_app");
  admin.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO edupilot_app");
  admin.update("INSERT INTO centers(id,name,slug,status) VALUES (101,'Center One','center-one','ACTIVE'),(202,'Center Two','center-two','ACTIVE')");
  admin.update("INSERT INTO students(center_id,student_number,first_name,last_name) VALUES (101,'S-101','One','Student'),(202,'S-202','Two','Student')");
  admin.update("INSERT INTO users(id,email,password_hash,first_name,last_name) VALUES (601,'parent-one@example.test','not-a-real-hash','Parent','One'),(602,'parent-two@example.test','not-a-real-hash','Parent','Two')");
  admin.update("INSERT INTO parent_student_links(center_id,parent_user_id,student_id) VALUES (101,601,1),(202,602,2)");
  admin.update("INSERT INTO notifications(center_id,recipient_user_id,type,title,message) VALUES (101,601,'GRADE','Grade update','Center one notification'),(202,602,'GRADE','Grade update','Center two notification')");
  admin.update("INSERT INTO audit_logs(center_id,action,entity_type,details) VALUES (101,'LOGIN','User','center one'),(202,'LOGIN','User','center two')");
  admin.update("INSERT INTO teachers(id,center_id,teacher_code,first_name,last_name) VALUES (301,101,'T-101','One','Teacher'),(302,202,'T-202','Two','Teacher')");
  admin.update("INSERT INTO courses(id,center_id,course_code,name) VALUES (401,101,'C-101','One Course'),(402,202,'C-202','Two Course')");
  admin.update("INSERT INTO study_groups(id,center_id,course_id,teacher_id,group_code,name) VALUES (501,101,401,301,'G-101','One Group'),(502,202,402,302,'G-202','Two Group')");
  admin.update("INSERT INTO lessons(id,center_id,group_id,title,starts_at,ends_at) VALUES (701,101,501,'One Lesson','2026-01-01 10:00:00','2026-01-01 11:00:00'),(702,202,502,'Two Lesson','2026-01-01 10:00:00','2026-01-01 11:00:00')");
  admin.update("INSERT INTO group_enrollments(center_id,group_id,student_id) VALUES (101,501,1),(202,502,2)");
  DataSource appDs=dataSource("edupilot_app","app-password");
  app=new JdbcTemplate(appDs);
  appTransaction=new TransactionTemplate(new DataSourceTransactionManager(appDs));
 }
 private static DataSource dataSource(String user,String password) {
  DriverManagerDataSource ds=new DriverManagerDataSource(postgres.getJdbcUrl(),user,password);
  ds.setDriverClassName("org.postgresql.Driver");return ds;
 }
 @Test void missingTenantContextCannotReadTenantRows() {
  Integer visible=app.queryForObject("SELECT COUNT(*) FROM students",Integer.class);
  assertEquals(0,visible);
 }
 @Test void transactionLocalTenantContextOnlyExposesSelectedCenter() {
  Integer visible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM students",Integer.class);
  });
  assertEquals(1,visible);
  Integer foreignVisible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM students WHERE center_id=202",Integer.class);
  });
  assertEquals(0,foreignVisible);
 }
 @Test void parentLinksAreRestrictedByTenantPolicy() {
  Integer visible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM parent_student_links",Integer.class);
  });
  assertEquals(1,visible);
  Integer foreignVisible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM parent_student_links WHERE center_id=202",Integer.class);
  });
  assertEquals(0,foreignVisible);
 }
 @Test void notificationsAreRestrictedByTenantPolicy() {
  Integer visible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class);
  });
  assertEquals(1,visible);
  Integer foreignVisible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM notifications WHERE center_id=202",Integer.class);
  });
  assertEquals(0,foreignVisible);
 }
 @Test void auditLogsAreRestrictedByTenantPolicy() {
  Integer visible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class);
  });
  assertEquals(1,visible);
 }
 @Test void groupEnrollmentRowsAreRestrictedByTenantPolicy() {
  Integer visible=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM group_enrollments",Integer.class);
  });
  assertEquals(1,visible);
 }
 @Test void inactiveEnrollmentCanBeReplacedButOnlyOneActiveEnrollmentIsAllowed() {
  Integer activeRows=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE group_enrollments SET active=FALSE WHERE center_id=101 AND group_id=501 AND student_id=1");
   app.update("INSERT INTO group_enrollments(center_id,group_id,student_id,active) VALUES (101,501,1,TRUE)");
   Integer active=app.queryForObject("SELECT COUNT(*) FROM group_enrollments WHERE center_id=101 AND group_id=501 AND student_id=1 AND active=TRUE",Integer.class);
   Integer inactive=app.queryForObject("SELECT COUNT(*) FROM group_enrollments WHERE center_id=101 AND group_id=501 AND student_id=1 AND active=FALSE",Integer.class);
   assertEquals(1,active);
   assertEquals(1,inactive);
   status.setRollbackOnly();
   return active;
  });
  assertEquals(1,activeRows);
 }
 @Test void duplicateActiveEnrollmentIsRejectedByDatabaseConstraint() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO group_enrollments(center_id,group_id,student_id,active) VALUES (101,501,1,TRUE)");
   return null;
  }));
 }
 @Test void enrollmentCannotReferenceGroupFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO group_enrollments(center_id,group_id,student_id,active) VALUES (101,502,1,TRUE)");
   return null;
  }));
 }
 @Test void enrollmentCannotReferenceStudentFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO group_enrollments(center_id,group_id,student_id,active) VALUES (101,501,2,TRUE)");
   return null;
  }));
 }
 @Test void groupCannotReferenceCourseFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO study_groups(center_id,course_id,teacher_id,group_code,name) VALUES (101,402,301,'G-BAD-COURSE','Invalid')");
   return null;
  }));
 }
 @Test void groupCannotReferenceTeacherFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO study_groups(center_id,course_id,teacher_id,group_code,name) VALUES (101,401,302,'G-BAD-TEACHER','Invalid')");
   return null;
  }));
 }
 @Test void lessonCannotReferenceGroupFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO lessons(center_id,group_id,title,starts_at,ends_at) VALUES (101,502,'Invalid Lesson','2026-01-02 10:00:00','2026-01-02 11:00:00')");
   return null;
  }));
 }
 @Test void attendanceCannotReferenceStudentFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO attendance_records(center_id,lesson_id,student_id,status) VALUES (101,701,2,'PRESENT')");
   return null;
  }));
 }
 @Test void gradeCannotReferenceStudentFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO grade_records(center_id,student_id,group_id,title,score,max_score) VALUES (101,2,501,'Invalid',8,10)");
   return null;
  }));
 }
 @Test void gradeCannotReferenceGroupFromAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO grade_records(center_id,student_id,group_id,title,score,max_score) VALUES (101,1,502,'Invalid',8,10)");
   return null;
  }));
 }
 @Test void tenantCannotInsertRowsForAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO students(center_id,student_number,first_name,last_name) VALUES (202,'S-HACK','Cross','Tenant')");
   return null;
  }));
 }
}
