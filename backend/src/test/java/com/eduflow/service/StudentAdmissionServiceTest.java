package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentAdmissionRequest;
import com.eduflow.entity.*;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.*;
import com.eduflow.security.TenantRlsContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAdmissionServiceTest {
    @Mock private CenterRepository centers;
    @Mock private StudentRepository students;
    @Mock private StudyGroupRepository groups;
    @Mock private UserRepository users;
    @Mock private CenterMembershipRepository memberships;
    @Mock private GroupEnrollmentRepository enrollments;
    @Mock private TenantRlsContext tenantRlsContext;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StudentAdmissionService service;

    @Test
    void admitsStudentCreatesLoginAndAssignsSelectedGroupTeacher() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        center.setId(1L);
        Teacher teacher = new Teacher(center, "T-01", "Olim", "Karimov", null, null, null);
        Course course = new Course(center, "ENG-01", "English", null);
        StudyGroup group = new StudyGroup(center, course, teacher, "A-01", "English A1", null, null);
        group.setId(12L);

        when(centers.findById(1L)).thenReturn(Optional.of(center));
        when(students.existsByCenter_IdAndStudentNumber(1L, "S-007")).thenReturn(false);
        when(groups.findByIdAndCenter_IdAndActiveTrue(12L, 1L)).thenReturn(Optional.of(group));
        when(students.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(55L);
            return student;
        });
        when(users.existsByEmail("student-1-55@students.edupilot.local")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return user;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$test-hash");

        StudentAdmissionRequest request = new StudentAdmissionRequest(
                " S-007 ", " Ali ", " Valiyev ", null, "+998901234567",
                LocalDate.of(2008, 5, 10), 12L
        );

        var result = service.admit(1L, request);

        assertEquals(55L, result.studentId());
        assertEquals("S-007", result.studentNumber());
        assertEquals("English A1", result.groupName());
        assertEquals("Olim Karimov", result.teacherName());
        assertEquals("student-1-55@students.edupilot.local", result.login());
        assertEquals(14, result.temporaryPassword().length());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertEquals("$2a$test-hash", createdUser.getPasswordHash());
        assertTrue(createdUser.isStudentAccount());
        assertTrue(createdUser.isMustChangePassword());

        ArgumentCaptor<CenterMembership> membershipCaptor = ArgumentCaptor.forClass(CenterMembership.class);
        verify(memberships).save(membershipCaptor.capture());
        assertEquals(Role.STUDENT, membershipCaptor.getValue().getRole());
        assertEquals(createdUser, membershipCaptor.getValue().getUser());

        ArgumentCaptor<GroupEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(GroupEnrollment.class);
        verify(enrollments).save(enrollmentCaptor.capture());
        assertEquals(group, enrollmentCaptor.getValue().getGroup());
        assertEquals(55L, enrollmentCaptor.getValue().getStudent().getId());
        verify(tenantRlsContext).apply(1L);
    }

    @Test
    void rejectsDuplicateStudentNumberBeforeCreatingAccount() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        when(centers.findById(1L)).thenReturn(Optional.of(center));
        when(students.existsByCenter_IdAndStudentNumber(1L, "S-007")).thenReturn(true);

        StudentAdmissionRequest request = new StudentAdmissionRequest(
                "S-007", "Ali", "Valiyev", null, null, null, 12L
        );

        assertThrows(EduFlowException.class, () -> service.admit(1L, request));

        verifyNoInteractions(groups, users, memberships, enrollments, passwordEncoder);
        verify(students, never()).save(any(Student.class));
    }

    @Test
    void rejectsInactiveOrForeignGroupWithoutCreatingStudent() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        when(centers.findById(1L)).thenReturn(Optional.of(center));
        when(students.existsByCenter_IdAndStudentNumber(1L, "S-007")).thenReturn(false);
        when(groups.findByIdAndCenter_IdAndActiveTrue(99L, 1L)).thenReturn(Optional.empty());

        StudentAdmissionRequest request = new StudentAdmissionRequest(
                "S-007", "Ali", "Valiyev", null, null, null, 99L
        );

        assertThrows(EduFlowException.class, () -> service.admit(1L, request));

        verify(students, never()).save(any(Student.class));
        verifyNoInteractions(users, memberships, enrollments, passwordEncoder);
    }
}
