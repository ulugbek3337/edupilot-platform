package com.eduflow.controller;
import com.eduflow.dto.PortalDtos.AuditResponse; import com.eduflow.entity.AuditLog; import com.eduflow.entity.Role; import com.eduflow.security.*; import com.eduflow.service.AuditLogService; import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/audit-logs") @RequiredArgsConstructor @PreAuthorize("hasRole('USER')")
public class AuditLogController {
 private final AuditLogService service; private final CenterMembershipService memberships;
 @GetMapping public List<AuditResponse> list(@AuthenticationPrincipal AuthenticatedUser p){Long c=CurrentCenterContext.get();if(c==null)throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST,"tenant.context.required");memberships.assertRoleForUserId(p.id(),c,Role.CENTER_ADMIN);return service.list(c);}
}
