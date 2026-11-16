package com.eduflow.controller;

import com.eduflow.dto.StudentDtos.StudentSelfResponse;
import com.eduflow.entity.Role;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student-portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StudentPortalController {
    private final StudentPortalService portal;
    private final CenterMembershipService memberships;

    @GetMapping("/me")
    public StudentSelfResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) {
            throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        memberships.assertRoleForUserId(principal.id(), centerId, Role.STUDENT);
        return portal.me(centerId, principal.id());
    }
}
