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
 @Test void tenantCannotInsertRowsForAnotherCenter() {
  assertThrows(org.springframework.dao.DataAccessException.class,()->appTransaction.execute(status->{
   app.queryForObject("SELECT set_config('app.current_center_id', ?, true)",String.class,"101");
   app.update("INSERT INTO students(center_id,student_number,first_name,last_name) VALUES (202,'S-HACK','Cross','Tenant')");
   return null;
  }));
 }
}
