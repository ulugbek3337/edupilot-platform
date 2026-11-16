package com.eduflow.controller;

import com.eduflow.dto.AcademicDtos.AttendanceRequest;
import com.eduflow.dto.AcademicDtos.GradeRequest;
import com.eduflow.dto.TeacherPortalDtos.*;
import com.eduflow.entity.Role;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.TeacherPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class TeacherPortalController {
    private final TeacherPortalService portal;
    private final CenterMembershipService memberships;

    private Long center(AuthenticatedUser user) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) throw new com.eduflow.exception.EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        memberships.assertRoleForUserId(user.id(), centerId, Role.TEACHER);
        return centerId;
    }

    @GetMapping("/me")
    public TeacherProfile me(@AuthenticationPrincipal AuthenticatedUser user) {
        return portal.me(center(user), user.id());
    }

    @GetMapping("/groups")
    public List<GroupItem> groups(@AuthenticationPrincipal AuthenticatedUser user) {
        return portal.myGroups(center(user), user.id());
    }

    @GetMapping("/groups/{groupId}/lessons")
    public List<LessonItem> lessons(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long groupId) {
        return portal.lessons(center(user), user.id(), groupId);
    }

    @GetMapping("/groups/{groupId}/students")
    public List<StudentItem> students(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long groupId) {
        return portal.students(center(user), user.id(), groupId);
    }

    @GetMapping("/lessons/{lessonId}/attendance")
    public List<AttendanceItem> attendance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long lessonId) {
        return portal.lessonAttendance(center(user), user.id(), lessonId);
    }

    @PostMapping("/attendance")
    public org.springframework.http.ResponseEntity<Void> markAttendance(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AttendanceRequest request) {
        Long centerId = center(user);
        portal.markAttendance(centerId, user.id(), request);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping("/grades")
    public org.springframework.http.ResponseEntity<Void> addGrade(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody GradeRequest request) {
        Long centerId = center(user);
        portal.addGrade(centerId, user.id(), request);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
