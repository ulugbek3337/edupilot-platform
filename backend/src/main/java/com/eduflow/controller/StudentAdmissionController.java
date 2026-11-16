package com.eduflow.controller;

import com.eduflow.dto.StudentDtos.StudentAdmissionRequest;
import com.eduflow.dto.StudentDtos.StudentAdmissionResponse;
import com.eduflow.entity.Role;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.AuditLogService;
import com.eduflow.service.StudentAdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student-admissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StudentAdmissionController {
    private final StudentAdmissionService admissions;
    private final CenterMembershipService memberships;
    private final AuditLogService audit;

    @PostMapping
    public ResponseEntity<StudentAdmissionResponse> admit(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StudentAdmissionRequest request) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) {
            throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        memberships.assertRoleForUserId(principal.id(), centerId, Role.CENTER_ADMIN);
        StudentAdmissionResponse result = admissions.admit(centerId, request);
        audit.record(centerId, principal.id(), "STUDENT_ADMITTED", "Student",
                result.studentId(), "Student enrolled in group " + result.groupId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
