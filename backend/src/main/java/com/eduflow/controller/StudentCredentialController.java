package com.eduflow.controller;

import com.eduflow.dto.StudentDtos.StudentCredentialResetResponse;
import com.eduflow.entity.Role;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.AuditLogService;
import com.eduflow.service.StudentCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StudentCredentialController {
    private final StudentCredentialService credentials;
    private final CenterMembershipService memberships;
    private final AuditLogService audit;

    @PostMapping("/{studentId}/reset-credentials")
    public ResponseEntity<StudentCredentialResetResponse> reset(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long studentId) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) {
            throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        memberships.assertRoleForUserId(principal.id(), centerId, Role.CENTER_ADMIN);
        StudentCredentialResetResponse result = credentials.reset(centerId, studentId);
        audit.record(centerId, principal.id(), "STUDENT_CREDENTIALS_RESET", "Student",
                studentId, "Temporary student credentials regenerated; previous sessions revoked");
        return ResponseEntity.ok(result);
    }
}
