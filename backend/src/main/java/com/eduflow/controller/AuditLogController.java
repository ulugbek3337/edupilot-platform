package com.eduflow.controller;

import com.eduflow.dto.PortalDtos.AuditResponse;
import com.eduflow.entity.Role;
import com.eduflow.exception.EduFlowException;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.AuditLogService;
import com.eduflow.service.CenterMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class AuditLogController {
    private final AuditLogService service;
    private final CenterMembershipService memberships;

    @GetMapping
    public List<AuditResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        memberships.assertRoleForUserId(principal.id(), centerId, Role.CENTER_ADMIN);
        return service.list(centerId, action, entityType, actorUserId, from, to);
    }
}
