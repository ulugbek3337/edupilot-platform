package com.eduflow.controller;

import com.eduflow.dto.StudentDtos.StudentRequest;
import com.eduflow.dto.StudentDtos.StudentResponse;
import com.eduflow.entity.Role;
import com.eduflow.entity.Student;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.security.CenterMembershipService;
import com.eduflow.security.CurrentCenterContext;
import com.eduflow.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StudentController {

    private final StudentService studentService;
    private final CenterMembershipService membershipService;

    @GetMapping
    public ResponseEntity<List<StudentResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Long centerId = requireCenter(principal.id());
        return ResponseEntity.ok(studentService.list(centerId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StudentRequest request) {
        Long centerId = requireCenter(principal.id());
        Student student = studentService.create(centerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(student));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long studentId) {
        Long centerId = requireCenter(principal.id());
        return ResponseEntity.ok(toResponse(studentService.get(centerId, studentId)));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentRequest request) {
        Long centerId = requireCenter(principal.id());
        return ResponseEntity.ok(toResponse(studentService.update(centerId, studentId, request)));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long studentId) {
        Long centerId = requireCenter(principal.id());
        membershipService.assertRoleForUserId(principal.id(), centerId, Role.CENTER_ADMIN);
        studentService.deactivate(centerId, studentId);
        return ResponseEntity.noContent().build();
    }

    private Long requireCenter(Long userId) {
        Long centerId = CurrentCenterContext.get();
        if (centerId == null) {
            throw new com.eduflow.exception.EduFlowException(
                    HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        membershipService.assertRoleForUserId(userId, centerId, Role.CENTER_ADMIN, Role.TEACHER);
        return centerId;
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(student.getId(), student.getStudentNumber(),
                student.getFirstName(), student.getLastName(), student.getEmail(),
                student.getPhone(), student.getDateOfBirth(), student.getStatus().name(),
                student.getVersion(), student.getCreatedAt(), student.getUpdatedAt());
    }
}
