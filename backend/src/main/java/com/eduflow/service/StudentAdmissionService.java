package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentAdmissionRequest;
import com.eduflow.dto.StudentDtos.StudentAdmissionResponse;
import com.eduflow.entity.*;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.*;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class StudentAdmissionService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CenterRepository centers;
    private final StudentRepository students;
    private final StudyGroupRepository groups;
    private final UserRepository users;
    private final CenterMembershipRepository memberships;
    private final GroupEnrollmentRepository enrollments;
    private final TenantRlsContext tenantRlsContext;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentAdmissionResponse admit(Long centerId, StudentAdmissionRequest request) {
        Center center = centers.findById(centerId)
                .filter(c -> c.getStatus() == CenterStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "center.not-found"));
        tenantRlsContext.apply(centerId);

        String number = request.studentNumber().trim();
        if (students.existsByCenter_IdAndStudentNumber(centerId, number)) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.number.exists");
        }
        StudyGroup group = groups.findByIdAndCenter_IdAndActiveTrue(request.groupId(), centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "group.not-found"));

        Student student = new Student(center, number, request.firstName().trim(), request.lastName().trim(),
                normalize(request.email()), normalize(request.phone()), request.dateOfBirth());
        student = students.save(student);

        String login = "student-" + centerId + "-" + student.getId() + "@students.edupilot.local";
        tenantRlsContext.applyUserEmail(login);
        if (users.existsByEmail(login)) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.login.exists");
        }
        String temporaryPassword = generatePassword();
        User user = new User(login, passwordEncoder.encode(temporaryPassword),
                student.getFirstName(), student.getLastName());
        user.setStudentAccount(true);
        user.setMustChangePassword(true);
        user = users.save(user);

        // The tenant- and role-aware foreign key requires the membership to exist
        // before the student account is attached to the student record.
        memberships.save(new CenterMembership(center, user, Role.STUDENT));
        student.setUser(user);
        students.save(student);
        enrollments.save(new GroupEnrollment(center, group, student));

        String teacherName = group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName();
        return new StudentAdmissionResponse(student.getId(), student.getStudentNumber(),
                student.getFirstName(), student.getLastName(), group.getId(), group.getName(),
                teacherName, login, temporaryPassword);
    }

    private String generatePassword() {
        StringBuilder value = new StringBuilder(14);
        for (int i = 0; i < 14; i++) value.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        return value.toString();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
