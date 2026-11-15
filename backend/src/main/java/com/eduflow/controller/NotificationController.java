package com.eduflow.controller;
import com.eduflow.dto.PortalDtos.NotificationResponse; import com.eduflow.entity.Notification; import com.eduflow.security.*; import com.eduflow.service.NotificationService; import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor @PreAuthorize("hasRole('USER')")
public class NotificationController {
 private final NotificationService service; private final CenterMembershipService memberships;
 private Long center(AuthenticatedUser p){Long c=CurrentCenterContext.get();if(c==null)throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST,"tenant.context.required");memberships.assertAccessForUserId(p.id(),c);return c;}
 private NotificationResponse response(Notification n){return new NotificationResponse(n.getId(),n.getType(),n.getTitle(),n.getMessage(),n.getCreatedAt(),n.getReadAt());}
 @GetMapping public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser p){return service.list(center(p),p.id()).stream().map(this::response).toList();}
 @PatchMapping("/{notificationId}/read") public NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedUser p,@PathVariable Long notificationId){return response(service.markRead(center(p),p.id(),notificationId));}
}
