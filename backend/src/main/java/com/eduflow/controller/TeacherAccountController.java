package com.eduflow.controller;

import com.eduflow.dto.TeacherPortalDtos.AccountCredentials;
import com.eduflow.dto.TeacherPortalDtos.AccountRequest;
import com.eduflow.entity.Role;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.TeacherPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class TeacherAccountController {
    private final TeacherPortalService portal;
    private final CenterMembershipService memberships;

    @PostMapping("/{teacherId}/account")
    public ResponseEntity<AccountCredentials> createAccount(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long teacherId, @RequestBody AccountRequest request) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        memberships.assertRoleForUserId(user.id(), centerId, Role.CENTER_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portal.createAccount(centerId, teacherId, request.email()));
    }
}
