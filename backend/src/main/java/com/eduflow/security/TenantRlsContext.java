package com.eduflow.security;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
@Component
@RequiredArgsConstructor
public class TenantRlsContext {
 private final JdbcTemplate jdbcTemplate;
 public void apply(Long centerId) {
  if(centerId==null) throw new IllegalArgumentException("Tenant center ID is required");
  jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
   if(!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return null;
   try(PreparedStatement statement=connection.prepareStatement("select set_config('app.current_center_id', ?, true)")) {
    statement.setString(1,centerId.toString()); statement.execute();
   }
   return null;
  });
 }
}
