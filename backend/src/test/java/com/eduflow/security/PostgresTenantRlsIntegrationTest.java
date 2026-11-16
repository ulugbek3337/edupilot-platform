package com.eduflow.security;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.zaxxer.hikari.HikariDataSource;
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
 static HikariDataSource appDataSource;
 static TransactionTemplate appTransaction;

 @BeforeAll static void migrateAndSeed() {
  DataSource adminDs=dataSource("postgres","postgres");
  admin=new JdbcTemplate(adminDs);
  admin.execute("REVOKE CREATE ON SCHEMA public FROM PUBLIC");
  admin.execute("CREATE ROLE edupilot_migrator LOGIN PASSWORD 'migration-password' NOSUPERUSER NOCREATEDB NOCREATEROLE BYPASSRLS");
  admin.execute("CREATE ROLE edupilot_app LOGIN PASSWORD 'app-password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS");
  admin.execute("GRANT CONNECT ON DATABASE edupilot_rls TO edupilot_migrator, edupilot_app");
  admin.execute("GRANT USAGE, CREATE ON SCHEMA public TO edupilot_migrator");
  admin.execute("GRANT USAGE ON SCHEMA public TO edupilot_app");
  admin.execute("ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO edupilot_app");
  admin.execute("ALTER DEFAULT PRIVILEGES FOR ROLE edupilot_migrator IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO edupilot_app");
  Flyway.configure().dataSource(postgres.getJdbcUrl(),"edupilot_migrator","migration-password")
   .locations("classpath:db/migration").load().migrate();
  admin.update("INSERT INTO centers(id,name,slug,status) VALUES (101,'Center One','center-one','ACTIVE'),(202,'Center Two','center-two','ACTIVE')");
  admin.update("INSERT INTO students(center_id,student_number,first_name,last_name) VALUES (101,'S-101','One','Student'),(202,'S-202','Two','Student')");
  admin.update("INSERT INTO users(id,email,password_hash,first_name,last_name) VALUES (601,'parent-one@example.test','not-a-real-hash','Parent','One'),(602,'parent-two@example.test','not-a-real-hash','Parent','Two'),(603,'student-one@example.test','not-a-real-hash','Student','One'),(604,'student-two@example.test','not-a-real-hash','Student','Two'),(605,'teacher-one@example.test','not-a-real-hash','Teacher','One'),(606,'teacher-two@example.test','not-a-real-hash','Teacher','Two'),(607,'wrong-role@example.test','not-a-real-hash','Wrong','Role'),(608,'new-member@example.test','not-a-real-hash','New','Member')");
  admin.update("INSERT INTO center_memberships(center_id,user_id,role) VALUES (101,601,'PARENT'),(202,602,'PARENT'),(101,603,'STUDENT'),(202,604,'STUDENT'),(101,605,'TEACHER'),(202,606,'TEACHER'),(101,607,'PARENT')");
  admin.update("UPDATE students SET user_id=603 WHERE center_id=101 AND student_number='S-101'");
  admin.update("UPDATE students SET user_id=604 WHERE center_id=202 AND student_number='S-202'");
  admin.update("INSERT INTO parent_student_links(center_id,parent_user_id,student_id) VALUES (101,601,1),(202,602,2)");
  admin.update("INSERT INTO notifications(center_id,recipient_user_id,type,title,message) VALUES (101,601,'GRADE','Grade update','Center one notification'),(202,602,'GRADE','Grade update','Center two notification')");
  admin.update("INSERT INTO audit_logs(center_id,action,entity_type,details) VALUES (101,'LOGIN','User','center one'),(202,'LOGIN','User','center two')");
  admin.update("INSERT INTO teachers(id,center_id,teacher_code,first_name,last_name,user_id) VALUES (301,101,'T-101','One','Teacher',605),(302,202,'T-202','Two','Teacher',606)");
  admin.update("INSERT INTO courses(id,center_id,course_code,name) VALUES (401,101,'C-101','One Course'),(402,202,'C-202','Two Course')");
  admin.update("INSERT INTO study_groups(id,center_id,course_id,teacher_id,group_code,name) VALUES (501,101,401,301,'G-101','One Group'),(502,202,402,302,'G-202','Two Group')");
  admin.update("INSERT INTO lessons(id,center_id,group_id,title,starts_at,ends_at) VALUES (701,101,501,'One Lesson','2026-01-01 10:00:00','2026-01-01 11:00:00'),(702,202,502,'Two Lesson','2026-01-01 10:00:00','2026-01-01 11:00:00')");
  admin.update("INSERT INTO group_enrollments(center_id,group_id,student_id) VALUES (101,501,1),(202,502,2)");
  admin.update("INSERT INTO refresh_tokens(token,user_id,revoked,expires_at) VALUES ('" + "a".repeat(64) + "',601,FALSE,CURRENT_TIMESTAMP + INTERVAL '1 day'),('" + "b".repeat(64) + "',602,FALSE,CURRENT_TIMESTAMP + INTERVAL '1 day')");
  appDataSource=new HikariDataSource();
  appDataSource.setJdbcUrl(postgres.getJdbcUrl());
  appDataSource.setUsername("edupilot_app");
  appDataSource.setPassword("app-password");
  appDataSource.setMaximumPoolSize(1);
  appDataSource.setMinimumIdle(1);
  app=new JdbcTemplate(appDataSource);
  appTransaction=new TransactionTemplate(new DataSourceTransactionManager(appDataSource));
 }
 @AfterAll static void closePool() {
  if(appDataSource!=null) appDataSource.close();
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
 @Test void notificationRecipientMustBelongToTheSameCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO notifications(center_id,recipient_user_id,type,title,message) VALUES (101,602,'GRADE','Invalid recipient','Cross-tenant')");
   return null;
  }));
 }
 @Test void notificationCannotBeCreatedForUserWithoutCenterMembership() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO notifications(center_id,recipient_user_id,type,title,message) VALUES (101,999,'GRADE','Invalid recipient','No membership')");
   return null;
  }));
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

 @Test void studentAccountMustBelongToSameCenterAndHaveStudentRole() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE students SET user_id=604 WHERE center_id=101 AND id=1");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE students SET user_id=607 WHERE center_id=101 AND id=1");
   return null;
  }));
 }
 @Test void teacherAccountMustBelongToSameCenterAndHaveTeacherRole() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE teachers SET user_id=606 WHERE center_id=101 AND id=301");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE teachers SET user_id=607 WHERE center_id=101 AND id=301");
   return null;
  }));
 }
 @Test void parentLinkMustUseSameCenterParentMembershipWithParentRole() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO parent_student_links(center_id,parent_user_id,student_id) VALUES (101,602,1)");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO parent_student_links(center_id,parent_user_id,student_id) VALUES (101,603,1)");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO parent_student_links(center_id,parent_user_id,student_id) VALUES (101,601,2)");
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

 @Test void globalUserRowsAreHiddenWithoutAnIdentityOrTenantContext() {
  Integer visible=app.queryForObject("SELECT COUNT(*) FROM users",Integer.class);
  assertEquals(0,visible);
 }

 @Test void unauthenticatedLoginLookupCanSeeOnlyTheSubmittedEmail() {
  Integer own=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_email', ?, true)",String.class,"parent-one@example.test");
   return app.queryForObject("SELECT COUNT(*) FROM users",Integer.class);
  });
  Integer other=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_email', ?, true)",String.class,"parent-one@example.test");
   return app.queryForObject("SELECT COUNT(*) FROM users WHERE email='parent-two@example.test'",Integer.class);
  });
  assertEquals(1,own);
  assertEquals(0,other);
 }

 @Test void authenticatedUserCanReadOnlyTheirOwnGlobalIdentity() {
  Integer own=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM users",Integer.class);
  });
  Integer other=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM users WHERE id=602",Integer.class);
  });
  assertEquals(1,own);
  assertEquals(0,other);
 }

 @Test void selectedTenantCanReadOnlyItsOwnMemberIdentities() {
  Integer ownTenant=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM users",Integer.class);
  });
  Integer otherTenant=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM users WHERE id=602",Integer.class);
  });
  assertEquals(4,ownTenant);
  assertEquals(0,otherTenant);
 }

 @Test void userIdentityInsertRequiresMatchingEmailContext() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.update("INSERT INTO users(email,password_hash,first_name,last_name) VALUES ('forbidden@example.test','hash','No','Context')");
   return null;
  }));
  Integer inserted=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_email', ?, true)",String.class,"created-by-email@example.test");
   return app.update("INSERT INTO users(email,password_hash,first_name,last_name) VALUES ('created-by-email@example.test','hash','Scoped','User')");
  });
  assertEquals(1,inserted);
 }

 @Test void globalUserUpdatesCannotReachAnotherIdentity() {
  Integer changed=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.update("UPDATE users SET first_name='Tampered' WHERE id=602");
  });
  assertEquals(0,changed);
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   app.update("UPDATE users SET id=602 WHERE id=601");
   return null;
  }));
 }

 @Test void membershipRowsAreHiddenWithoutUserOrCenterContext() {
  Integer visible=app.queryForObject("SELECT COUNT(*) FROM center_memberships",Integer.class);
  assertEquals(0,visible);
 }

 @Test void authenticatedUserCanReadOnlyTheirOwnMembershipsBeforeCenterSelection() {
  Integer own=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM center_memberships",Integer.class);
  });
  Integer foreign=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM center_memberships WHERE user_id=602",Integer.class);
  });
  assertEquals(1,own);
  assertEquals(0,foreign);
 }

 @Test void selectedCenterCanReadOnlyMembershipsInThatCenter() {
  Integer ownCenter=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM center_memberships",Integer.class);
  });
  Integer otherCenter=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.queryForObject("SELECT COUNT(*) FROM center_memberships WHERE center_id=202",Integer.class);
  });
  assertEquals(4,ownCenter);
  assertEquals(0,otherCenter);
 }

 @Test void membershipWritesRequireSelectedCenterContext() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   app.update("INSERT INTO center_memberships(center_id,user_id,role) VALUES (101,608,'STUDENT')");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO center_memberships(center_id,user_id,role) VALUES (202,608,'STUDENT')");
   return null;
  }));
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE center_memberships SET center_id=202 WHERE center_id=101 AND user_id=601");
   return null;
  }));
 }

 @Test void membershipDeleteCannotAffectAnotherCenter() {
  Integer deleted=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.update("DELETE FROM center_memberships WHERE center_id=202");
  });
  assertEquals(0,deleted);
  Integer stillThere=admin.queryForObject(
   "SELECT COUNT(*) FROM center_memberships WHERE center_id=202 AND user_id=602",Integer.class);
  assertEquals(1,stillThere);
 }

 @Test void refreshTokensAreHiddenWithoutAuthenticatedUserContext() {
  Integer visible=app.queryForObject("SELECT COUNT(*) FROM refresh_tokens",Integer.class);
  assertEquals(0,visible);
 }

 @Test void refreshTokenRowsAreScopedToAuthenticatedUser() {
  Integer own=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM refresh_tokens",Integer.class);
  });
  Integer foreign=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id=602",Integer.class);
  });
  assertEquals(1,own);
  assertEquals(0,foreign);
 }

 @Test void refreshTokensCannotBeCreatedForAnotherUser() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   app.update("INSERT INTO refresh_tokens(token,user_id,revoked,expires_at) VALUES ('" + "c".repeat(64) + "',602,FALSE,CURRENT_TIMESTAMP + INTERVAL '1 day')");
   return null;
  }));
 }

 @Test void refreshTokenUpdatesAndDeletesCannotTouchAnotherUser() {
  Integer updated=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.update("UPDATE refresh_tokens SET revoked=TRUE WHERE user_id=602");
  });
  Integer deleted=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.update("DELETE FROM refresh_tokens WHERE user_id=602");
  });
  assertEquals(0,updated);
  assertEquals(0,deleted);
  Integer stillThere=admin.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id=602",Integer.class);
  assertEquals(1,stillThere);
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   app.update("UPDATE refresh_tokens SET user_id=602 WHERE user_id=601");
   return null;
  }));
 }

 @Test void runtimeRoleCannotReadOrChangeFlywayMigrationHistory() {
  assertThrows(org.springframework.dao.DataAccessException.class,
   ()->app.queryForObject("SELECT COUNT(*) FROM flyway_schema_history",Integer.class));
 }

 @Test void applicationDatabaseRoleCannotBypassRowLevelSecurity() {
  Boolean bypass=admin.queryForObject("SELECT rolbypassrls OR rolsuper FROM pg_roles WHERE rolname='edupilot_app'",Boolean.class);
  assertEquals(Boolean.FALSE,bypass,"Application DB role must not be superuser or BYPASSRLS.");
 }

 @Test void applicationDatabaseRoleMustNotOwnTenantTablesOrCreateSchemaObjects() {
  Integer ownedTables=admin.queryForObject("""
   SELECT COUNT(*)
   FROM pg_class c
   JOIN pg_namespace n ON n.oid=c.relnamespace
   JOIN pg_roles r ON r.oid=c.relowner
   WHERE n.nspname='public'
     AND c.relkind IN ('r','p')
     AND r.rolname='edupilot_app'
   """,Integer.class);
  assertEquals(0,ownedTables,"Runtime role must not own tables; Flyway must use the migration role.");

  Boolean canCreate=admin.queryForObject(
   "SELECT has_schema_privilege('edupilot_app','public','CREATE')",Boolean.class);
  assertEquals(Boolean.FALSE,canCreate,"Runtime role must not be able to create or replace schema objects.");
  Boolean runtimeBypass=admin.queryForObject(
   "SELECT rolbypassrls FROM pg_roles WHERE rolname='edupilot_app'",Boolean.class);
  assertEquals(Boolean.FALSE,runtimeBypass);
 }

 @Test void migrationRoleCanBypassRlsButIsNotSuperuser() {
  Boolean safeMigrationRole=admin.queryForObject(
   "SELECT rolbypassrls AND NOT rolsuper FROM pg_roles WHERE rolname='edupilot_migrator'",Boolean.class);
  assertEquals(Boolean.TRUE,safeMigrationRole);
 }
 @Test void updateCannotReadOrModifyAnotherTenantsRows() {
  Integer changed=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.update("UPDATE students SET first_name='Tampered' WHERE center_id=202");
  });
  assertEquals(0,changed);
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("UPDATE students SET center_id=202 WHERE center_id=101 AND student_number='S-101'");
   return null;
  }));
 }
 @Test void deleteCannotAffectAnotherTenantsRows() {
  Integer deleted=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   return app.update("DELETE FROM students WHERE center_id=202");
  });
  assertEquals(0,deleted);
  Integer stillThere=admin.queryForObject("SELECT COUNT(*) FROM students WHERE center_id=202 AND student_number='S-202'",Integer.class);
  assertEquals(1,stillThere);
 }
 @Test void tenantAndUserContextsDoNotLeakAcrossReusedPoolConnection() {
  Integer first=appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.queryForObject("SELECT set_config('app.current_user_id', ?, true)",String.class,"601");
   return app.queryForObject("SELECT COUNT(*) FROM students",Integer.class);
  });
  Integer secondStudents=appTransaction.execute(status->app.queryForObject("SELECT COUNT(*) FROM students",Integer.class));
  Integer secondMemberships=appTransaction.execute(status->app.queryForObject("SELECT COUNT(*) FROM center_memberships",Integer.class));
  Integer secondTokens=appTransaction.execute(status->app.queryForObject("SELECT COUNT(*) FROM refresh_tokens",Integer.class));
  assertEquals(1,first);
  assertEquals(0,secondStudents,"SET LOCAL center context must be cleared at transaction completion.");
  assertEquals(0,secondMemberships,"SET LOCAL user context must be cleared at transaction completion.");
  assertEquals(0,secondTokens,"A prior user's refresh-token context must not leak to the next request.");
 }
}
